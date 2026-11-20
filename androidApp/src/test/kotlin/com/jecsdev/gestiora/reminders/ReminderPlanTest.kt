package com.jecsdev.gestiora.reminders

import com.jecsdev.gestiora.domain.models.*
import org.junit.Assert.*
import org.junit.Test
import java.util.GregorianCalendar
import java.util.Calendar
import java.util.TimeZone

class ReminderPlanTest {
    @Test fun lateDeliveryRequiresPriorScheduleAndHonorsExpiryAndDeduplication() {
        val row = PlannedReminder("appointment", "one", dueAt = 1_000, expiresAt = 10_000)
        assertTrue(dueReminders(listOf(row), 900, setOf(row.token), emptySet()).isEmpty())
        assertTrue(dueReminders(listOf(row), 1_100, emptySet(), emptySet()).isEmpty())
        assertEquals(listOf(row), dueReminders(listOf(row), 1_100, setOf(row.token), emptySet()))
        assertTrue(dueReminders(listOf(row), 1_100, setOf(row.token), setOf(row.token)).isEmpty())
        assertTrue(dueReminders(listOf(row), 10_000, setOf(row.token), emptySet()).isEmpty())
        assertTrue(dueReminders(listOf(row.copy(expiresAt = Long.MAX_VALUE)), 86_402_000, setOf(row.token), emptySet()).isEmpty())
    }
    private val utc = TimeZone.getTimeZone("UTC")
    private fun appointment(reminder: AppointmentReminder = AppointmentReminder.ONE_DAY) = Appointment(
        "a", "2028-02-29", "10:30", "Dr", "", "", "", AppointmentStatus.SCHEDULED, reminder = reminder,
    )
    private fun millis(zone: TimeZone, year: Int, month: Int, day: Int, hour: Int, minute: Int = 0) = GregorianCalendar(zone).apply {
        clear(); set(year, month - 1, day, hour, minute)
    }.timeInMillis

    @Test fun allAppointmentOffsetsAndEligibility() {
        val offsets = mapOf(AppointmentReminder.ONE_HOUR to millis(utc, 2028, 2, 29, 9, 30),
            AppointmentReminder.ONE_DAY to millis(utc, 2028, 2, 28, 10, 30),
            AppointmentReminder.TWO_DAYS to millis(utc, 2028, 2, 27, 10, 30),
            AppointmentReminder.ONE_WEEK to millis(utc, 2028, 2, 22, 10, 30))
        offsets.forEach { (offset, expected) -> assertEquals(expected, reminderPlan(listOf(appointment(offset)), emptyList(), utc).single().dueAt) }
        val excluded = listOf(appointment(AppointmentReminder.NONE), appointment().copy(status = AppointmentStatus.CANCELLED), appointment().copy(status = AppointmentStatus.COMPLETED))
        assertTrue(reminderPlan(excluded, emptyList(), utc).isEmpty())
    }

    @Test fun examsOptInOnlyAndNoInventedExamTime() {
        val base = ExamRecord("e", "Exam", scheduled = "2028-03-01", status = ExamStatus.SCHEDULED, reminder = true)
        val planned = reminderPlan(emptyList(), listOf(base), utc).single()
        assertEquals(millis(utc, 2028, 2, 29, 9), planned.dueAt)
        assertEquals(millis(utc, 2028, 3, 2, 0), planned.expiresAt)
        assertTrue(reminderPlan(emptyList(), listOf(base.copy(reminder = false), base.copy(scheduled = null),
            base.copy(status = ExamStatus.PENDING), base.copy(status = ExamStatus.COMPLETED)), utc).isEmpty())
    }

    @Test fun civilDayOffsetAcrossDstAndZoneChanges() {
        val ny = TimeZone.getTimeZone("America/New_York")
        val appointment = appointment().copy(date = "2028-03-12", time = "10:30")
        val plan = reminderPlan(listOf(appointment), emptyList(), ny).single()
        assertEquals(millis(ny, 2028, 3, 11, 10, 30), plan.dueAt)
        assertEquals(23 * 3_600_000L, plan.expiresAt - plan.dueAt)
        assertNotEquals(plan.token, reminderPlan(listOf(appointment), emptyList(), utc).single().token)
    }

    @Test fun identityStableButEditCreatesNewDeliveryToken() {
        val base = reminderPlan(listOf(appointment()), emptyList(), utc).single()
        assertEquals(base.token, reminderPlan(listOf(appointment().copy(doctor = "Updated")), emptyList(), utc).single().token)
        assertNotEquals(base.token, reminderPlan(listOf(appointment().copy(time = "12:00")), emptyList(), utc).single().token)
        assertEquals(64, base.token.length)
    }
}
