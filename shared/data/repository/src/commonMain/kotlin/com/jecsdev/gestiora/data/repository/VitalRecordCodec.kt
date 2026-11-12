package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.domain.models.VitalRecord
import com.jecsdev.gestiora.domain.models.VitalType
import com.jecsdev.gestiora.domain.models.VitalRecordValidation

class CorruptVitalRecordsException : IllegalStateException("Stored vital records are corrupt")
class UnsupportedVitalRecordsVersionException : IllegalStateException("Stored vital records version is unsupported")

internal object VitalRecordCodec {
    private const val HEADER = "GESTIORA_VITALS:1\n"
    fun encode(records: List<VitalRecord>): String {
        require(records.size <= 1000 && records.map { it.id }.toSet().size == records.size)
        records.forEach(::validateVitalRecord)
        val raw = buildString {
            append(HEADER).append(records.size).append('\n')
            records.forEach { record ->
                listOf(record.id, record.date, record.type.name, record.value?.toString(), record.value2?.toString(), record.label, record.text, record.notes).forEach {
                    if (it == null) append("-1:") else append(it.length).append(':').append(it)
                }
            }
        }
        require(raw.length <= 1_000_000)
        return raw
    }

    fun decode(raw: String): List<VitalRecord> {
        if (raw.length > 1_000_000 || !raw.startsWith("GESTIORA_VITALS:")) throw CorruptVitalRecordsException()
        if (!raw.startsWith(HEADER)) throw UnsupportedVitalRecordsVersionException()
        val end = raw.indexOf('\n', HEADER.length)
        if (end < 0 || end - HEADER.length !in 1..4) throw CorruptVitalRecordsException()
        val countText = raw.substring(HEADER.length, end)
        if (countText.any { it !in '0'..'9' }) throw CorruptVitalRecordsException()
        val count = countText.toIntOrNull()?.takeIf { it in 0..1000 } ?: throw CorruptVitalRecordsException()
        var position = end + 1
        fun field(): String? {
            val colon = raw.indexOf(':', position)
            if (colon < 0 || colon - position !in 1..5) throw CorruptVitalRecordsException()
            val token = raw.substring(position, colon)
            if (token == "-1") { position = colon + 1; return null }
            if (token.any { it !in '0'..'9' }) throw CorruptVitalRecordsException()
            val length = token.toIntOrNull()?.takeIf { it <= 4096 } ?: throw CorruptVitalRecordsException()
            if (length > raw.length - colon - 1) throw CorruptVitalRecordsException()
            position = colon + 1 + length
            return raw.substring(colon + 1, position)
        }
        fun required() = field() ?: throw CorruptVitalRecordsException()
        fun number(): Double? = field()?.let { it.toDoubleOrNull() ?: throw CorruptVitalRecordsException() }
        val records = List(count) {
            val id = required()
            val date = required()
            val typeName = required()
            val type = VitalType.entries.firstOrNull { it.name == typeName } ?: throw CorruptVitalRecordsException()
            VitalRecord(id, date, type, number(), number(), field(), field(), field())
        }
        if (position != raw.length || records.map { it.id }.toSet().size != records.size) throw CorruptVitalRecordsException()
        try { records.forEach(::validateVitalRecord) } catch (_: IllegalArgumentException) { throw CorruptVitalRecordsException() }
        return records
    }
}

internal fun validateVitalRecord(record: VitalRecord) {
    require(record.id.isNotBlank() && record.id.length <= 128)
    require(validCivilDate(record.date))
    VitalRecordValidation.validate(record)
}
