package com.jecsdev.gestiora.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.text.format.DateFormat
import com.jecsdev.gestiora.GestioraApplication
import com.jecsdev.gestiora.MainActivity
import com.jecsdev.gestiora.R
import com.jecsdev.gestiora.domain.models.Appointment
import com.jecsdev.gestiora.domain.models.ExamRecord
import com.jecsdev.gestiora.domain.models.PersonalReminder
import com.jecsdev.gestiora.domain.models.ReminderKind
import java.util.Date
import java.util.TimeZone

internal class AndroidReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val state = context.getSharedPreferences("local_reminder_delivery_v1", Context.MODE_PRIVATE)
    private fun alarm() = PendingIntent.getBroadcast(context, 0,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_DUE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun reconcile(
        appointments: List<Appointment>,
        exams: List<ExamRecord>,
        personalReminders: List<PersonalReminder>,
        deliverDue: Boolean,
    ) {
        val channel = createChannel(context)
        val now = System.currentTimeMillis()
        val plan = reminderPlan(appointments, exams, TimeZone.getDefault(), personalReminders)
        val tokens = plan.map { it.token }.toSet()
        val delivered = state.getStringSet("delivered", emptySet()).orEmpty().intersect(tokens).toMutableSet()
        val armed = state.getStringSet("armed", emptySet()).orEmpty()
        val posted = state.getStringSet("posted", emptySet()).orEmpty().toMutableSet()
        (posted - tokens).forEach { notifications.cancel(it, 0); posted.remove(it) }
        if (deliverDue && notificationsEnabled(context)) {
            val appointmentsById = appointments.associateBy { it.id }
            val examsById = exams.associateBy { it.id }
            val personalById = personalReminders.associateBy { it.id }
            dueReminders(plan, now, armed, delivered).forEach { item ->
                val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val (title, body) = when (item.kind) {
                    "appointment" -> {
                        val appointment = appointmentsById.getValue(item.id)
                        val event = Date(item.expiresAt)
                        val date = DateFormat.getMediumDateFormat(context).format(event)
                        val time = DateFormat.getTimeFormat(context).format(event)
                        val details = if (appointment.doctor.isBlank()) {
                            context.getString(R.string.notification_appointment_time, date, time)
                        } else {
                            context.getString(R.string.notification_appointment_doctor, appointment.doctor, date, time)
                        }
                        R.string.notification_appointment_title to details
                    }
                    "exam" -> R.string.notification_exam_title to examsById.getValue(item.id).name
                    else -> {
                        val reminder = personalById.getValue(item.id)
                        val label = if (reminder.kind == ReminderKind.MEDICATION) R.string.notification_medication_title
                            else R.string.notification_personal_title
                        label to reminder.title
                    }
                }
                val redacted = notificationBuilder(channel)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(R.string.notification_public))
                    .build()
                val notification = notificationBuilder(channel).setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(title))
                    .setContentText(body)
                    .setStyle(Notification.BigTextStyle().bigText(body))
                    .setPublicVersion(redacted)
                    .setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(true)
                    .setVisibility(Notification.VISIBILITY_PRIVATE).setCategory(Notification.CATEGORY_REMINDER).build()
                notifications.notify(item.token, 0, notification)
                delivered.add(item.token)
                posted.add(item.token)
            }
        }
        // One alarm points to the next future reminder; no titles or medical data in intents/preferences.
        val future = plan.filter { it.dueAt > now && it.token !in delivered }
        val overdue = if (deliverDue) emptyList() else dueReminders(plan, now, armed, delivered)
        check(state.edit().putStringSet("delivered", delivered).putStringSet("posted", posted)
            .putStringSet("armed", (future + overdue).map { it.token }.toSet()).commit())
        alarms.cancel(alarm())
        (future + overdue).minOfOrNull { it.dueAt.coerceAtLeast(now + 1_000) }?.let(::schedule)
    }

    private fun notificationBuilder(channel: String): Notification.Builder =
        if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, channel)
        else Notification.Builder(context).setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)

    fun retryLater() { schedule(System.currentTimeMillis() + 15 * 60_000) }
    private fun schedule(at: Long) { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarm()) }

    companion object {
        private const val LEGACY_CHANNEL = "scheduled_reminders_v1"
        private const val CHANNEL = "scheduled_reminders_v2"
        const val ACTION_DUE = "com.jecsdev.gestiora.REMINDER_DUE"

        fun createChannel(context: Context): String {
            if (Build.VERSION.SDK_INT < 26) return CHANNEL
            val manager = context.getSystemService(NotificationManager::class.java)
            val selected = channelId(context)
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            manager.createNotificationChannel(NotificationChannel(
                selected, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                enableVibration(true)
                setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            })
            // Keep already-posted alerts visible until they are dismissed.
            if (selected == CHANNEL && manager.getNotificationChannel(LEGACY_CHANNEL) != null &&
                manager.activeNotifications.none { it.notification.channelId == LEGACY_CHANNEL }) {
                manager.deleteNotificationChannel(LEGACY_CHANNEL)
            }
            return selected
        }

        fun channelId(context: Context): String {
            if (Build.VERSION.SDK_INT < 26) return CHANNEL
            val manager = context.getSystemService(NotificationManager::class.java)
            val legacy = manager.getNotificationChannel(LEGACY_CHANNEL) ?: return CHANNEL
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val customized = legacy.importance != NotificationManager.IMPORTANCE_DEFAULT ||
                legacy.sound != defaultSound || legacy.shouldVibrate() || !legacy.canShowBadge() ||
                (Build.VERSION.SDK_INT >= 29 && legacy.hasUserSetImportance()) ||
                (Build.VERSION.SDK_INT >= 30 && legacy.hasUserSetSound())
            // Channel behavior is immutable after registration. Upgrade only the old defaults.
            return if (customized) LEGACY_CHANNEL else CHANNEL
        }
        fun appNotificationsEnabled(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
            return context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        }
        fun notificationsEnabled(context: Context): Boolean {
            val manager = context.getSystemService(NotificationManager::class.java)
            return appNotificationsEnabled(context) &&
                (Build.VERSION.SDK_INT < 26 ||
                    manager.getNotificationChannel(channelId(context))?.importance != NotificationManager.IMPORTANCE_NONE)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        (context.applicationContext as GestioraApplication).refreshReminders(
            deliverDue = intent.action == AndroidReminderScheduler.ACTION_DUE,
            finished = pending::finish,
        )
    }
}
