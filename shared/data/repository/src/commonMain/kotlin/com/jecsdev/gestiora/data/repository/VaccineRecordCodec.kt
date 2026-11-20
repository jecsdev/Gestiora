package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.domain.models.VaccineRecord
import com.jecsdev.gestiora.domain.models.VaccineStatus

class CorruptVaccineRecordsException : IllegalStateException("Stored records are corrupt")
class UnsupportedVaccineRecordsVersionException : IllegalStateException("Stored records version is unsupported")

internal object VaccineRecordCodec {
    private const val HEADER = "GESTIORA_VACCINES:1\n"
    fun encode(records: List<VaccineRecord>): String {
        require(records.size <= 1000 && records.map { it.id }.toSet().size == records.size)
        records.forEach(::validateVaccineRecord)
        val raw = buildString {
            append(HEADER).append(records.size).append('\n')
            records.forEach { record ->
                listOf(record.id, record.name, record.dose, record.scheduled, record.administered, record.status.name, record.provider, record.location, record.proof, record.notes).forEach {
                    if (it == null) append("-1:") else append(it.length).append(':').append(it)
                }
            }
        }
        require(raw.length <= 1_000_000)
        return raw
    }

    fun decode(raw: String): List<VaccineRecord> {
        if (raw.length > 1_000_000 || !raw.startsWith("GESTIORA_VACCINES:")) throw CorruptVaccineRecordsException()
        if (!raw.startsWith(HEADER)) throw UnsupportedVaccineRecordsVersionException()
        val end = raw.indexOf('\n', HEADER.length)
        if (end < 0 || end - HEADER.length !in 1..4) throw CorruptVaccineRecordsException()
        val countText = raw.substring(HEADER.length, end)
        if (countText.any { it !in '0'..'9' }) throw CorruptVaccineRecordsException()
        val count = countText.toIntOrNull()?.takeIf { it in 0..1000 } ?: throw CorruptVaccineRecordsException()
        var position = end + 1
        fun field(): String? {
            val colon = raw.indexOf(':', position)
            if (colon < 0 || colon - position !in 1..5) throw CorruptVaccineRecordsException()
            val token = raw.substring(position, colon)
            if (token == "-1") { position = colon + 1; return null }
            if (token.any { it !in '0'..'9' }) throw CorruptVaccineRecordsException()
            val length = token.toIntOrNull()?.takeIf { it <= 4096 } ?: throw CorruptVaccineRecordsException()
            if (length > raw.length - colon - 1) throw CorruptVaccineRecordsException()
            position = colon + 1 + length
            return raw.substring(colon + 1, position)
        }
        fun required() = field() ?: throw CorruptVaccineRecordsException()
        val records = List(count) {
            val id = required()
            val name = required()
            val dose = field()
            val scheduled = field()
            val administered = field()
            val statusName = required()
            val status = VaccineStatus.entries.firstOrNull { it.name == statusName } ?: throw CorruptVaccineRecordsException()
            VaccineRecord(id, name, dose, scheduled, administered, status, field(), field(), field(), field())
        }
        if (position != raw.length || records.map { it.id }.toSet().size != records.size) throw CorruptVaccineRecordsException()
        try { records.forEach(::validateVaccineRecord) } catch (_: IllegalArgumentException) { throw CorruptVaccineRecordsException() }
        return records
    }
}

internal fun validateVaccineRecord(record: VaccineRecord) {
    require(record.id.isNotBlank() && record.id.length <= 128)
    require(record.name.isNotBlank() && record.name.length <= 512)
    require(listOf(record.dose, record.provider, record.location, record.proof).all { (it?.length ?: 0) <= 512 })
    require((record.notes?.length ?: 0) <= 4096)
    record.scheduled?.let { require(validCivilDate(it)) }
    record.administered?.let { require(validCivilDate(it)) }
    if (record.status == VaccineStatus.ADMINISTERED) require(record.administered != null)
}
