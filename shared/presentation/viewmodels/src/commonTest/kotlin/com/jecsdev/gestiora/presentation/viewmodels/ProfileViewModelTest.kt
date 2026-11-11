package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.DatingMethod
import com.jecsdev.gestiora.domain.models.PregnancyDating
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.domain.repository.PregnancyProfileRepository
import com.jecsdev.gestiora.domain.usecases.CalculatePregnancyAge
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileViewModelTest {
    private val profile = PregnancyProfile(country = "Bolivia", dating = PregnancyDating(DatingMethod.FUM, lmp = "2026-01-01"))

    @Test fun readFailureDoesNotBecomeEmptyProfile() = runBlocking {
        val repository = FakeRepository().apply { failRead = true }
        val viewModel = ProfileViewModel(repository, CalculatePregnancyAge())
        assertEquals(ProfileLoad.Error, viewModel.load())
        assertEquals(0, repository.saves)
    }

    @Test fun successfulSaveAndReload() = runBlocking {
        val repository = FakeRepository()
        val viewModel = ProfileViewModel(repository, CalculatePregnancyAge())
        assertEquals(ProfileLoad.Empty, viewModel.load())
        assertTrue(viewModel.save(profile))
        assertEquals(ProfileLoad.Ready(profile), viewModel.load())
    }

    @Test fun failedSaveReturnsFailureWithoutClaimingSuccess() = runBlocking {
        val repository = FakeRepository().apply { failWrite = true }
        val viewModel = ProfileViewModel(repository, CalculatePregnancyAge())
        assertFalse(viewModel.save(profile))
        assertEquals(ProfileLoad.Empty, viewModel.load())
    }

    private class FakeRepository : PregnancyProfileRepository {
        var stored: PregnancyProfile? = null
        var failRead = false
        var failWrite = false
        var saves = 0

        override suspend fun load(): PregnancyProfile? {
            if (failRead) error("read failed")
            return stored
        }

        override suspend fun save(profile: PregnancyProfile) {
            if (failWrite) error("write failed")
            saves++
            stored = profile
        }

        override suspend fun clear() { stored = null }
    }
}
