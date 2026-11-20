package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.domain.models.MedicalCondition
import com.jecsdev.gestiora.domain.models.ConditionStatus

class CorruptMedicalConditionsException : IllegalStateException("Stored conditions are corrupt")
class UnsupportedMedicalConditionsVersionException : IllegalStateException("Stored conditions version is unsupported")

internal object MedicalConditionCodec {
    private const val HEADER = "GESTIORA_CONDITIONS:1\n"
    fun encode(records: List<MedicalCondition>): String {
        require(records.size <= 1000 && records.map { it.id }.toSet().size == records.size)
        records.forEach(::validateMedicalCondition)
        val raw = buildString {
            append(HEADER).append(records.size).append('\n')
            records.forEach { record ->
                listOf(record.id, record.name, record.date, record.professional, record.instructions,
                    record.treatment, record.monitoring, record.followUp, record.status.name, record.notes).forEach {
                    if (it == null) append("-1:") else append(it.length).append(':').append(it)
                }
            }
        }
        require(raw.length <= 1_000_000)
        return raw
    }

    fun decode(raw: String): List<MedicalCondition> {
        if (raw.length > 1_000_000 || !raw.startsWith("GESTIORA_CONDITIONS:")) throw CorruptMedicalConditionsException()
        if (!raw.startsWith(HEADER)) throw UnsupportedMedicalConditionsVersionException()
        val end = raw.indexOf('\n', HEADER.length)
        if (end < 0 || end - HEADER.length !in 1..4) throw CorruptMedicalConditionsException()
        val countText = raw.substring(HEADER.length, end)
        if (countText.any { it !in '0'..'9' }) throw CorruptMedicalConditionsException()
        val count = countText.toIntOrNull()?.takeIf { it in 0..1000 } ?: throw CorruptMedicalConditionsException()
        var position = end + 1
        fun field(): String? {
            val colon = raw.indexOf(':', position)
            if (colon < 0 || colon - position !in 1..5) throw CorruptMedicalConditionsException()
            val token = raw.substring(position, colon)
            if (token == "-1") { position = colon + 1; return null }
            if (token.any { it !in '0'..'9' }) throw CorruptMedicalConditionsException()
            val length = token.toIntOrNull()?.takeIf { it <= 4096 } ?: throw CorruptMedicalConditionsException()
            if (length > raw.length - colon - 1) throw CorruptMedicalConditionsException()
            position = colon + 1 + length
            return raw.substring(colon + 1, position)
        }
        fun required() = field() ?: throw CorruptMedicalConditionsException()
        val records = List(count) {
            val id = required()
            val name = required()
            val date = required()
            val professional = required()
            val instructions = field()
            val treatment = field()
            val monitoring = field()
            val followUp = field()
            val statusName = required()
            val status = ConditionStatus.entries.firstOrNull { it.name == statusName } ?: throw CorruptMedicalConditionsException()
            MedicalCondition(id, name, date, professional, instructions, treatment, monitoring, followUp, status, field())
        }
        if (position != raw.length || records.map { it.id }.toSet().size != records.size) throw CorruptMedicalConditionsException()
        try { records.forEach(::validateMedicalCondition) } catch (_: IllegalArgumentException) { throw CorruptMedicalConditionsException() }
        return records
    }
}

internal fun validateMedicalCondition(record: MedicalCondition) {
    require(record.id.isNotBlank() && record.id.length <= 128)
    require(record.name.isNotBlank() && record.name.length <= 512)
    require(record.professional.isNotBlank() && record.professional.length <= 512)
    require(validCivilDate(record.date))
    require(listOf(record.instructions, record.treatment, record.monitoring, record.notes).all { (it?.length ?: 0) <= 4096 })
    record.followUp?.let { require(validCivilDate(it)) }
}
