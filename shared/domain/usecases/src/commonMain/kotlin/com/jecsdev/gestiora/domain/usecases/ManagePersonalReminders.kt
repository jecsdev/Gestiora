package com.jecsdev.gestiora.domain.usecases

import com.jecsdev.gestiora.domain.models.PersonalReminder
import com.jecsdev.gestiora.domain.repository.PersonalReminderRepository
import kotlin.random.Random

/** Separates generated local identities from editable reminder fields. */
class ManagePersonalReminders(private val repository: PersonalReminderRepository) {
    suspend fun list(): List<PersonalReminder> = repository.list()

    suspend fun add(reminder: PersonalReminder) {
        val id = Random.Default.nextBytes(16).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        repository.add(reminder.copy(id = id))
    }

    suspend fun update(reminder: PersonalReminder) = repository.update(reminder)
    suspend fun delete(id: String) = repository.delete(id)
}
