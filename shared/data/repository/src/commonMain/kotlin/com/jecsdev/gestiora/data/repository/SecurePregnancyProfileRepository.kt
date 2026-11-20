package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureProfileStore
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.domain.repository.PregnancyProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SecurePregnancyProfileRepository(private val store: SecureProfileStore) : PregnancyProfileRepository {
    private val mutex = Mutex()

    override suspend fun load(): PregnancyProfile? = mutex.withLock {
        withContext(Dispatchers.Default) { store.read()?.let(ProfileCodec::decode) }
    }

    override suspend fun save(profile: PregnancyProfile) = mutex.withLock {
        withContext(Dispatchers.Default) {
            // Check existing content before writing. Corrupt or unavailable data must never be replaced silently.
            store.read()?.let(ProfileCodec::decode)
            store.write(ProfileCodec.encode(profile))
        }
    }

    override suspend fun clear() = mutex.withLock {
        withContext(Dispatchers.Default) { store.clear() }
    }
}
