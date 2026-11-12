package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.domain.models.Consultation

class CorruptConsultationsException : IllegalStateException("Stored consultations are corrupt")
class UnsupportedConsultationsVersionException : IllegalStateException("Stored consultations version is unsupported")

internal object ConsultationCodec {
    private const val HEADER = "GESTIORA_CONSULTATIONS:1\n"
    fun encode(records: List<Consultation>): String {
        require(records.size <= 1000 && records.map { it.id }.toSet().size == records.size)
        records.forEach(::validateConsultation)
        val raw = buildString {
            append(HEADER).append(records.size).append('\n')
            records.forEach { record ->
                listOf(record.id, record.date, record.gestationalAge, record.doctor, record.observations,
                    record.instructions, record.medications, record.exams, record.followUp, record.nextDate).forEach {
                    if (it == null) append("-1:") else append(it.length).append(':').append(it)
                }
            }
        }
        require(raw.length <= 1_000_000)
        return raw
    }

    fun decode(raw: String): List<Consultation> {
        if (raw.length > 1_000_000 || !raw.startsWith("GESTIORA_CONSULTATIONS:")) throw CorruptConsultationsException()
        if (!raw.startsWith(HEADER)) throw UnsupportedConsultationsVersionException()
        val end = raw.indexOf('\n', HEADER.length)
        if (end < 0 || end - HEADER.length !in 1..4) throw CorruptConsultationsException()
        val countText = raw.substring(HEADER.length, end)
        if (countText.any { it !in '0'..'9' }) throw CorruptConsultationsException()
        val count = countText.toIntOrNull()?.takeIf { it in 0..1000 } ?: throw CorruptConsultationsException()
        var position = end + 1
        fun field(): String? {
            val colon = raw.indexOf(':', position)
            if (colon < 0 || colon - position !in 1..5) throw CorruptConsultationsException()
            val token = raw.substring(position, colon)
            if (token == "-1") { position = colon + 1; return null }
            if (token.any { it !in '0'..'9' }) throw CorruptConsultationsException()
            val length = token.toIntOrNull()?.takeIf { it <= 4096 } ?: throw CorruptConsultationsException()
            if (length > raw.length - colon - 1) throw CorruptConsultationsException()
            position = colon + 1 + length
            return raw.substring(colon + 1, position)
        }
        fun required() = field() ?: throw CorruptConsultationsException()
        val records = List(count) {
            Consultation(required(), required(), field(), field(), field(), field(), field(), field(), field(), field())
        }
        if (position != raw.length || records.map { it.id }.toSet().size != records.size) throw CorruptConsultationsException()
        try { records.forEach(::validateConsultation) } catch (_: IllegalArgumentException) { throw CorruptConsultationsException() }
        return records
    }
}

internal fun validateConsultation(record: Consultation) {
    require(record.id.isNotBlank() && record.id.length <= 128)
    require(validCivilDate(record.date))
    require((record.gestationalAge?.length ?: 0) <= 512 && (record.doctor?.length ?: 0) <= 512)
    require(listOf(record.observations, record.instructions, record.medications, record.exams, record.followUp).all { (it?.length ?: 0) <= 4096 })
    record.nextDate?.let { require(validCivilDate(it)) }
}
