package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.domain.models.ExamRecord
import com.jecsdev.gestiora.domain.models.ExamStatus
import com.jecsdev.gestiora.domain.models.ExamKind

class CorruptExamRecordsException : IllegalStateException("Stored records are corrupt")
class UnsupportedExamRecordsVersionException : IllegalStateException("Stored records version is unsupported")

internal object ExamRecordCodec {
    private const val HEADER = "GESTIORA_EXAMS:1\n"
    fun encode(records: List<ExamRecord>): String {
        require(records.size <= 1000 && records.map { it.id }.toSet().size == records.size)
        records.forEach(::validateExamRecord)
        val raw = buildString {
            append(HEADER).append(records.size).append('\n')
            records.forEach { record ->
                listOf(record.id, record.name, record.kind.name, record.requested, record.scheduled, record.completed, record.status.name, record.physician, record.results, record.notes, record.attachment).forEach {
                    if (it == null) append("-1:") else append(it.length).append(':').append(it)
                }
            }
        }
        require(raw.length <= 1_000_000)
        return raw
    }

    fun decode(raw: String): List<ExamRecord> {
        if (raw.length > 1_000_000 || !raw.startsWith("GESTIORA_EXAMS:")) throw CorruptExamRecordsException()
        if (!raw.startsWith(HEADER)) throw UnsupportedExamRecordsVersionException()
        val end = raw.indexOf('\n', HEADER.length)
        if (end < 0 || end - HEADER.length !in 1..4) throw CorruptExamRecordsException()
        val countText = raw.substring(HEADER.length, end)
        if (countText.any { it !in '0'..'9' }) throw CorruptExamRecordsException()
        val count = countText.toIntOrNull()?.takeIf { it in 0..1000 } ?: throw CorruptExamRecordsException()
        var position = end + 1
        fun field(): String? {
            val colon = raw.indexOf(':', position)
            if (colon < 0 || colon - position !in 1..5) throw CorruptExamRecordsException()
            val token = raw.substring(position, colon)
            if (token == "-1") { position = colon + 1; return null }
            if (token.any { it !in '0'..'9' }) throw CorruptExamRecordsException()
            val length = token.toIntOrNull()?.takeIf { it <= 4096 } ?: throw CorruptExamRecordsException()
            if (length > raw.length - colon - 1) throw CorruptExamRecordsException()
            position = colon + 1 + length
            return raw.substring(colon + 1, position)
        }
        fun required() = field() ?: throw CorruptExamRecordsException()
        val records = List(count) {
            val id = required()
            val name = required()
            val kindName = required()
            val kind = ExamKind.entries.firstOrNull { it.name == kindName } ?: throw CorruptExamRecordsException()
            val requested = field()
            val scheduled = field()
            val completed = field()
            val statusName = required()
            val status = ExamStatus.entries.firstOrNull { it.name == statusName } ?: throw CorruptExamRecordsException()
            ExamRecord(id, name, kind, requested, scheduled, completed, status, field(), field(), field(), field())
        }
        if (position != raw.length || records.map { it.id }.toSet().size != records.size) throw CorruptExamRecordsException()
        try { records.forEach(::validateExamRecord) } catch (_: IllegalArgumentException) { throw CorruptExamRecordsException() }
        return records
    }
}

internal fun validateExamRecord(record: ExamRecord) {
    require(record.id.isNotBlank() && record.id.length <= 128)
    require(record.name.isNotBlank() && record.name.length <= 512)
    require(listOf(record.physician, record.attachment).all { (it?.length ?: 0) <= 512 })
    require(listOf(record.results, record.notes).all { (it?.length ?: 0) <= 4096 })
    record.requested?.let { require(validCivilDate(it)) }
    record.scheduled?.let { require(validCivilDate(it)) }
    record.completed?.let { require(validCivilDate(it)) }
    if (record.status == ExamStatus.COMPLETED) require(record.completed != null)
}
