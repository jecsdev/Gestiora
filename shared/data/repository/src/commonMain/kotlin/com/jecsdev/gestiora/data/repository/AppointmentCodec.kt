package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.domain.models.Appointment
import com.jecsdev.gestiora.domain.models.AppointmentStatus
import com.jecsdev.gestiora.domain.models.AppointmentReminder

class CorruptAppointmentsException : IllegalStateException("Stored appointments are corrupt")
class UnsupportedAppointmentsVersionException : IllegalStateException("Stored appointments version is unsupported")

/** Bounded, versioned length-prefixed format; fields may contain any character. */
internal object AppointmentCodec {
    private const val PREFIX = "GESTIORA_APPOINTMENTS:"
    private const val LEGACY_HEADER = "GESTIORA_APPOINTMENTS:1\n"
    private const val HEADER = "GESTIORA_APPOINTMENTS:2\n"
    private const val MAX_RECORDS = 1000
    private const val MAX_PAYLOAD = 1_000_000
    private const val MAX_FIELD = 4096

    fun encode(appointments: List<Appointment>): String {
        require(appointments.size <= MAX_RECORDS) { "Too many appointments" }
        require(appointments.map { it.id }.toSet().size == appointments.size) { "Duplicate appointment id" }
        appointments.forEach(::validateAppointment)
        val result = buildString {
            append(HEADER).append(appointments.size).append('\n')
            appointments.forEach { a ->
                listOf(a.id, a.date, a.time, a.doctor, a.specialty, a.clinic, a.reason, a.status.name, a.notes, a.reminder.name).forEach { value ->
                    if (value == null) append("-1:") else append(value.length).append(':').append(value)
                }
            }
        }
        require(result.length <= MAX_PAYLOAD) { "Appointments payload is too large" }
        return result
    }

    fun decode(raw: String): List<Appointment> {
        if (raw.length > MAX_PAYLOAD || !raw.startsWith(PREFIX)) throw CorruptAppointmentsException()
        val legacy = raw.startsWith(LEGACY_HEADER)
        if (!legacy && !raw.startsWith(HEADER)) throw UnsupportedAppointmentsVersionException()
        val headerLength = if (legacy) LEGACY_HEADER.length else HEADER.length
        val countEnd = raw.indexOf('\n', headerLength)
        if (countEnd < 0 || countEnd - headerLength !in 1..4) throw CorruptAppointmentsException()
        val countText = raw.substring(headerLength, countEnd)
        if (countText.any { it !in '0'..'9' }) throw CorruptAppointmentsException()
        val count = countText.toIntOrNull()?.takeIf { it <= MAX_RECORDS } ?: throw CorruptAppointmentsException()
        val cursor = Cursor(raw, countEnd + 1)
        val appointments = List(count) {
            val id = cursor.required()
            val date = cursor.required()
            val time = cursor.required()
            val doctor = cursor.required()
            val specialty = cursor.required()
            val clinic = cursor.required()
            val reason = cursor.required()
            val statusName = cursor.required()
            val status = AppointmentStatus.entries.firstOrNull { it.name == statusName }
            val notes = cursor.field()
            val reminder = if (legacy) AppointmentReminder.NONE else {
                val reminderName = cursor.required()
                AppointmentReminder.entries.firstOrNull { it.name == reminderName } ?: throw CorruptAppointmentsException()
            }
            Appointment(id, date, time, doctor, specialty, clinic, reason, status ?: throw CorruptAppointmentsException(), notes, reminder)
        }
        if (!cursor.atEnd() || appointments.map { it.id }.toSet().size != appointments.size) throw CorruptAppointmentsException()
        try { appointments.forEach(::validateAppointment) } catch (_: IllegalArgumentException) { throw CorruptAppointmentsException() }
        return appointments
    }

    private class Cursor(private val raw: String, private var position: Int) {
        fun required(): String = field() ?: throw CorruptAppointmentsException()
        fun field(): String? {
            val colon = raw.indexOf(':', position)
            if (colon < 0 || colon - position !in 1..5) throw CorruptAppointmentsException()
            val token = raw.substring(position, colon)
            if (token == "-1") { position = colon + 1; return null }
            if (token.any { it !in '0'..'9' }) throw CorruptAppointmentsException()
            val length = token.toIntOrNull()?.takeIf { it <= MAX_FIELD } ?: throw CorruptAppointmentsException()
            if (length > raw.length - colon - 1) throw CorruptAppointmentsException()
            position = colon + 1 + length
            return raw.substring(colon + 1, position)
        }
        fun atEnd() = position == raw.length
    }
}

internal fun validateAppointment(a: Appointment) {
    require(a.id.isNotBlank() && a.id.length <= 128) { "Invalid appointment id" }
    require(validCivilDate(a.date)) { "Invalid appointment date" }
    require(a.time.length == 5 && a.time[2] == ':' && a.time.indices.all { it == 2 || a.time[it] in '0'..'9' } &&
        a.time.substring(0, 2).toInt() in 0..23 && a.time.substring(3, 5).toInt() in 0..59) { "Invalid appointment time" }
    listOf(a.doctor, a.specialty, a.clinic, a.reason).forEach { require(it.isNotBlank() && it.length <= 512) { "Invalid appointment field" } }
    require((a.notes?.length ?: 0) <= 4096) { "Appointment notes too long" }
}
