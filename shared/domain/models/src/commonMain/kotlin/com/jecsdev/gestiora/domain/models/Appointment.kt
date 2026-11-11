package com.jecsdev.gestiora.domain.models

enum class AppointmentStatus { SCHEDULED, COMPLETED, CANCELLED }

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
)
