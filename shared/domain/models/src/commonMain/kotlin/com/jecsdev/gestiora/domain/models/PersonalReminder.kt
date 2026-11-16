package com.jecsdev.gestiora.domain.models

enum class ReminderKind { APPOINTMENT, EXAM, VACCINE, FOLLOW_UP, PERSONAL }

/** An in-app calendar entry; it does not schedule a system notification. */
data class PersonalReminder(
    val id: String,
    val title: String,
    val date: String,
    val time: String? = null,
    val kind: ReminderKind = ReminderKind.PERSONAL,
    val done: Boolean = false,
)
