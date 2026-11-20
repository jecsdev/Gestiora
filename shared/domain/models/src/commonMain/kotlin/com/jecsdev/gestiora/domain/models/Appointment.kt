package com.jecsdev.gestiora.domain.models

enum class AppointmentStatus { SCHEDULED, COMPLETED, CANCELLED }
enum class AppointmentReminder { NONE, ONE_HOUR, ONE_DAY, TWO_DAYS, ONE_WEEK }

/** Date and time are local civil values (yyyy-MM-dd and HH:mm), not UTC instants. */
data class Appointment(
    val id: String,
    val date: String,
    val time: String,
    val doctor: String,
    val specialty: String,
    val clinic: String,
    val reason: String,
    val status: AppointmentStatus,
    val notes: String? = null,
    val reminder: AppointmentReminder = AppointmentReminder.NONE,
)
