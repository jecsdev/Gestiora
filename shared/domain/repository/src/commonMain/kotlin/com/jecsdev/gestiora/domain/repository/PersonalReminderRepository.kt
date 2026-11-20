package com.jecsdev.gestiora.domain.repository

import com.jecsdev.gestiora.domain.models.PersonalReminder

/** Storage failures must throw; an empty list means that no reminders exist. */
interface PersonalReminderRepository {
    suspend fun list(): List<PersonalReminder>
    suspend fun add(reminder: PersonalReminder)
    suspend fun update(reminder: PersonalReminder)
    suspend fun delete(id: String)
}
