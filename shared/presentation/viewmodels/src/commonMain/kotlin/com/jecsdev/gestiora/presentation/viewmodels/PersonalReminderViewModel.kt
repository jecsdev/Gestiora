package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.PersonalReminder
import com.jecsdev.gestiora.domain.usecases.ManagePersonalReminders
import kotlin.coroutines.cancellation.CancellationException

class PersonalReminderViewModel(private val reminders: ManagePersonalReminders) {
    suspend fun load(): Result<List<PersonalReminder>> = capture { reminders.list() }
    suspend fun add(reminder: PersonalReminder): Result<Unit> = capture { reminders.add(reminder) }
    suspend fun update(reminder: PersonalReminder): Result<Unit> = capture { reminders.update(reminder) }
    suspend fun delete(id: String): Result<Unit> = capture { reminders.delete(id) }

    private suspend inline fun <T> capture(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}
