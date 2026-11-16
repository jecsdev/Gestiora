package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.domain.models.PersonalReminder
import com.jecsdev.gestiora.domain.models.ReminderKind

class CorruptPersonalRemindersException : IllegalStateException("Stored reminders are corrupt")
class UnsupportedPersonalRemindersVersionException : IllegalStateException("Stored reminders version is unsupported")

internal object PersonalReminderCodec {
    private const val HEADER = "GESTIORA_REMINDERS:1\n"
    fun encode(reminders: List<PersonalReminder>): String {
        require(reminders.size <= 1000 && reminders.map { it.id }.toSet().size == reminders.size)
        reminders.forEach(::validatePersonalReminder)
        val raw = buildString {
            append(HEADER).append(reminders.size).append('\n')
            reminders.forEach { reminder ->
                listOf(reminder.id, reminder.title, reminder.date, reminder.time, reminder.kind.name, reminder.done.toString()).forEach {
                    if (it == null) append("-1:") else append(it.length).append(':').append(it)
                }
            }
        }
        require(raw.length <= 1_000_000)
        return raw
    }

    fun decode(raw: String): List<PersonalReminder> {
        if (raw.length > 1_000_000 || !raw.startsWith("GESTIORA_REMINDERS:")) throw CorruptPersonalRemindersException()
        if (!raw.startsWith(HEADER)) throw UnsupportedPersonalRemindersVersionException()
        val end = raw.indexOf('\n', HEADER.length)
        if (end < 0 || end - HEADER.length !in 1..4) throw CorruptPersonalRemindersException()
        val countText = raw.substring(HEADER.length, end)
        if (countText.any { it !in '0'..'9' }) throw CorruptPersonalRemindersException()
        val count = countText.toIntOrNull()?.takeIf { it in 0..1000 } ?: throw CorruptPersonalRemindersException()
        var position = end + 1
        fun field(): String? {
            val colon = raw.indexOf(':', position)
            if (colon < 0 || colon - position !in 1..5) throw CorruptPersonalRemindersException()
            val token = raw.substring(position, colon)
            if (token == "-1") { position = colon + 1; return null }
            if (token.any { it !in '0'..'9' }) throw CorruptPersonalRemindersException()
            val length = token.toIntOrNull()?.takeIf { it <= 4096 } ?: throw CorruptPersonalRemindersException()
            if (length > raw.length - colon - 1) throw CorruptPersonalRemindersException()
            position = colon + 1 + length
            return raw.substring(colon + 1, position)
        }
        fun required() = field() ?: throw CorruptPersonalRemindersException()
        val reminders = List(count) {
            val id = required()
            val title = required()
            val date = required()
            val time = field()
            val kind = required()
            val done = when (required()) {
                "true" -> true
                "false" -> false
                else -> throw CorruptPersonalRemindersException()
            }
            PersonalReminder(id, title, date, time,
                ReminderKind.entries.firstOrNull { it.name == kind } ?: throw CorruptPersonalRemindersException(), done)
        }
        if (position != raw.length || reminders.map { it.id }.toSet().size != reminders.size) throw CorruptPersonalRemindersException()
        try { reminders.forEach(::validatePersonalReminder) } catch (_: IllegalArgumentException) { throw CorruptPersonalRemindersException() }
        return reminders
    }
}

internal fun validatePersonalReminder(reminder: PersonalReminder) {
    require(reminder.id.isNotBlank() && reminder.id.length <= 128)
    require(reminder.title.isNotBlank() && reminder.title.length <= 512)
    require(validCivilDate(reminder.date))
    reminder.time?.let {
        require(it.length == 5 && it[2] == ':' && it.filterIndexed { index, _ -> index != 2 }.all { c -> c in '0'..'9' } &&
            it.take(2).toIntOrNull() in 0..23 && it.takeLast(2).toIntOrNull() in 0..59)
    }
}
