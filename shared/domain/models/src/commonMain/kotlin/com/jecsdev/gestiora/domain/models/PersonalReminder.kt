package com.jecsdev.gestiora.domain.models

enum class ReminderKind { APPOINTMENT, EXAM, VACCINE, FOLLOW_UP, PERSONAL, MEDICATION }

/** A calendar reminder scheduled in local civil time on supported platforms. */
data class PersonalReminder(
    val id: String,
    val title: String,
    val date: String,
    val time: String? = null,
    val kind: ReminderKind = ReminderKind.PERSONAL,
    val done: Boolean = false,
)
