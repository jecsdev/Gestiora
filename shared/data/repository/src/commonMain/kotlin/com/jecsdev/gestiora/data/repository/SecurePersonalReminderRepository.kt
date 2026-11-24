package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecurePersonalRemindersStore
import com.jecsdev.gestiora.data.datasources.ScheduleChangeNotifier
import com.jecsdev.gestiora.domain.models.PersonalReminder
import com.jecsdev.gestiora.domain.repository.PersonalReminderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Each read-modify-write is one serialized operation; storage failures are never treated as empty. */
class SecurePersonalReminderRepository(
    private val store: SecurePersonalRemindersStore,
    private val scheduleChanges: ScheduleChangeNotifier = ScheduleChangeNotifier {},
) : PersonalReminderRepository {
    private val mutex = Mutex()

    override suspend fun list(): List<PersonalReminder> = mutex.withLock {
        withContext(Dispatchers.Default) { readAll() }
    }

    override suspend fun add(reminder: PersonalReminder) = mutate { existing ->
        validatePersonalReminder(reminder)
        require(existing.none { it.id == reminder.id }) { "PersonalReminder id already exists" }
        existing + reminder
    }

    override suspend fun update(reminder: PersonalReminder) = mutate { existing ->
        validatePersonalReminder(reminder)
        require(existing.any { it.id == reminder.id }) { "PersonalReminder not found" }
        existing.map { if (it.id == reminder.id) reminder else it }
    }

    override suspend fun delete(id: String) = mutate { existing ->
        require(existing.any { it.id == id }) { "PersonalReminder not found" }
        existing.filterNot { it.id == id }
    }

    private suspend fun mutate(change: (List<PersonalReminder>) -> List<PersonalReminder>) = mutex.withLock {
        withContext(Dispatchers.Default) {
            val current = readAll()
            store.write(PersonalReminderCodec.encode(change(current)))
            runCatching { scheduleChanges.requestReconcile() }
            Unit
        }
    }

    private fun readAll(): List<PersonalReminder> = store.read()?.let(PersonalReminderCodec::decode) ?: emptyList()
}
