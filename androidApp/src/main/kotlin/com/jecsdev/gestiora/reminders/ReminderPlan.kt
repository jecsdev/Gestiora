package com.jecsdev.gestiora.reminders

import com.jecsdev.gestiora.domain.models.*
import java.security.MessageDigest
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

internal data class PlannedReminder(val kind: String, val id: String, val dueAt: Long, val expiresAt: Long) {
    // Only an opaque digest is persisted outside encrypted clinical storage.
    val token: String = MessageDigest.getInstance("SHA-256")
        .digest("$kind\n$id\n$dueAt".toByteArray(Charsets.UTF_8))
        .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}

/** Never catch up reminders first discovered after their due time. Only previously armed items
 * can fire late (up to one day, and never after the recorded event has passed).
 */
internal fun dueReminders(plan: List<PlannedReminder>, now: Long, armed: Set<String>, delivered: Set<String>) =
    plan.filter { it.token in armed && it.token !in delivered && it.dueAt <= now && now < it.expiresAt && now - it.dueAt <= 86_400_000 }

/** Civil dates follow the current device zone. Calendar supports the app's API 24 minimum.
 * DST gaps roll forward; overlaps use Calendar's standard-time occurrence.
 */
internal fun reminderPlan(
    appointments: List<Appointment>, exams: List<ExamRecord>, zone: TimeZone,
): List<PlannedReminder> = buildList {
    fun civil(date: String, time: String): Calendar = GregorianCalendar(zone).apply {
        clear()
        set(date.substring(0, 4).toInt(), date.substring(5, 7).toInt() - 1, date.substring(8, 10).toInt(),
            time.substring(0, 2).toInt(), time.substring(3, 5).toInt(), 0)
        // Resolve daylight-saving gaps/overlaps before applying relative offsets.
        timeInMillis
    }
    appointments.filter { it.status == AppointmentStatus.SCHEDULED && it.reminder != AppointmentReminder.NONE }.forEach { item ->
        val event = civil(item.date, item.time)
        val expires = event.timeInMillis
        when (item.reminder) {
            AppointmentReminder.ONE_HOUR -> event.add(Calendar.HOUR_OF_DAY, -1)
            AppointmentReminder.ONE_DAY -> event.add(Calendar.DAY_OF_MONTH, -1)
            AppointmentReminder.TWO_DAYS -> event.add(Calendar.DAY_OF_MONTH, -2)
            AppointmentReminder.ONE_WEEK -> event.add(Calendar.DAY_OF_MONTH, -7)
            AppointmentReminder.NONE -> Unit
        }
        add(PlannedReminder("appointment", item.id, event.timeInMillis, expires))
    }
    exams.filter { it.status == ExamStatus.SCHEDULED && it.reminder && it.scheduled != null }.forEach { item ->
        val event = civil(requireNotNull(item.scheduled), "09:00")
        val expiry = civil(requireNotNull(item.scheduled), "00:00").apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        event.add(Calendar.DAY_OF_MONTH, -1)
        add(PlannedReminder("exam", item.id, event.timeInMillis, expiry))
    }
}.sortedBy { it.dueAt }
