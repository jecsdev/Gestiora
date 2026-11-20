package com.jecsdev.gestiora.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import com.jecsdev.gestiora.GestioraApplication
import com.jecsdev.gestiora.MainActivity
import com.jecsdev.gestiora.R
import com.jecsdev.gestiora.domain.models.Appointment
import com.jecsdev.gestiora.domain.models.ExamRecord
import java.util.TimeZone

internal class AndroidReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val state = context.getSharedPreferences("local_reminder_delivery_v1", Context.MODE_PRIVATE)
    private fun alarm() = PendingIntent.getBroadcast(context, 0,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_DUE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun reconcile(appointments: List<Appointment>, exams: List<ExamRecord>, deliverDue: Boolean) {
        createChannel(context)
        val now = System.currentTimeMillis()
        val plan = reminderPlan(appointments, exams, TimeZone.getDefault())
        val tokens = plan.map { it.token }.toSet()
        val delivered = state.getStringSet("delivered", emptySet()).orEmpty().intersect(tokens).toMutableSet()
        val armed = state.getStringSet("armed", emptySet()).orEmpty()
        val posted = state.getStringSet("posted", emptySet()).orEmpty().toMutableSet()
        (posted - tokens).forEach { notifications.cancel(it, 0); posted.remove(it) }
        if (deliverDue && notificationsEnabled(context)) {
            dueReminders(plan, now, armed, delivered).forEach { item ->
                val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL) else Notification.Builder(context)
                val notification = builder.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(context.getString(if (item.kind == "exam") R.string.notification_exam else R.string.notification_appointment))
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

    fun retryLater() { schedule(System.currentTimeMillis() + 15 * 60_000) }
    private fun schedule(at: Long) { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarm()) }

    companion object {
        const val CHANNEL = "scheduled_reminders_v1"
        const val ACTION_DUE = "com.jecsdev.gestiora.REMINDER_DUE"
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                })
        }
        fun notificationsEnabled(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
            val manager = context.getSystemService(NotificationManager::class.java)
            return manager.areNotificationsEnabled() && (Build.VERSION.SDK_INT < 26 || manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)
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
