package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.Appointment
import com.jecsdev.gestiora.domain.models.AppointmentStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class UpcomingAppointmentsTest {
    private fun appointment(id: String, date: String, time: String, status: AppointmentStatus = AppointmentStatus.SCHEDULED) =
        Appointment(id, date, time, "Médico", "Especialidad", "Clínica", "Control", status)

    @Test
    fun scheduledTodayAndFutureAreSortedAndLimited() {
        val appointments = listOf(
            appointment("future", "2026-11-14", "09:00"),
            appointment("late", "2026-11-12", "15:00"),
            appointment("past", "2026-11-11", "09:00"),
            appointment("cancelled", "2026-11-12", "08:00", AppointmentStatus.CANCELLED),
            appointment("completed", "2026-11-12", "08:00", AppointmentStatus.COMPLETED),
            appointment("early", "2026-11-12", "09:00"),
            appointment("tomorrow", "2026-11-13", "10:00"),
        )
        assertEquals(listOf("early", "late", "tomorrow"), upcomingAppointments(appointments, "2026-11-12").map { it.id })
        assertEquals(emptyList(), upcomingAppointments(appointments, "2026-11-15"))
    }
}
