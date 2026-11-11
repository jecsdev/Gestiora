package com.jecsdev.gestiora.domain.repository

import com.jecsdev.gestiora.domain.models.PregnancyProfile

interface PregnancyProfileRepository {
    /** Null means no profile exists. Storage failures must throw. */
    suspend fun load(): PregnancyProfile?
    suspend fun save(profile: PregnancyProfile)
    suspend fun clear()
}
