package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.PregnancyAge
import com.jecsdev.gestiora.domain.models.PregnancyDating
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.domain.repository.PregnancyProfileRepository
import com.jecsdev.gestiora.domain.usecases.CalculatePregnancyAge
import kotlin.coroutines.cancellation.CancellationException

sealed interface ProfileLoad {
    data object Loading : ProfileLoad
    data object Empty : ProfileLoad
    data class Ready(val profile: PregnancyProfile) : ProfileLoad
    data object Error : ProfileLoad
}

class ProfileViewModel(
    private val repository: PregnancyProfileRepository,
    private val calculate: CalculatePregnancyAge,
) {
    suspend fun load(): ProfileLoad = try {
        repository.load()?.let(ProfileLoad::Ready) ?: ProfileLoad.Empty
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ProfileLoad.Error
    }

    suspend fun save(profile: PregnancyProfile): Boolean = try {
        repository.save(profile)
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }

    fun preview(dating: PregnancyDating, at: String): PregnancyAge? =
        calculate(dating, at)?.takeIf(calculate::isOngoing)
}
