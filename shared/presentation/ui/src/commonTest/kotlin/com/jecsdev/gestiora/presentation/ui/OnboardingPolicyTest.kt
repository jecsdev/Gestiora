package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.DatingMethod
import com.jecsdev.gestiora.domain.models.PregnancyDating
import kotlin.test.*

class OnboardingPolicyTest {
    @Test fun backBlocksDuringSaveAndConfirmsLeavingProfileEditor() {
        for (step in 0..3) for (editing in listOf(false, true)) {
            assertEquals(OnboardingBackAction.BLOCK, onboardingBackAction(step, editing, true, false))
            assertEquals(OnboardingBackAction.BLOCK, onboardingBackAction(step, editing, true, true))
        }
        assertEquals(OnboardingBackAction.DISMISS_DISCARD, onboardingBackAction(1, true, false, true))
        assertEquals(OnboardingBackAction.CONFIRM_DISCARD, onboardingBackAction(1, true, false, false))
        assertEquals(OnboardingBackAction.PREVIOUS_STEP, onboardingBackAction(3, true, false, false))
        assertEquals(OnboardingBackAction.PREVIOUS_STEP, onboardingBackAction(2, false, false, false))
        assertEquals(OnboardingBackAction.PREVIOUS_STEP, onboardingBackAction(1, false, false, false))
        assertEquals(OnboardingBackAction.SYSTEM, onboardingBackAction(0, false, false, false))
    }

    @Test fun unchangedHistoricalDatesArePreservedWhenUpdatingPersonalDetails() {
        val original = PregnancyDating(DatingMethod.FUM, lmp = "2024-01-01", referenceDate = "2024-03-01", referenceWeeks = 8)
        val draft = PregnancyDating(DatingMethod.FUM, lmp = "2024-01-01")
        assertSame(original, profileDatingToSave(draft, original, ongoing = false))
        val ultrasound = PregnancyDating(DatingMethod.ULTRASOUND, referenceDate = "2024-03-01", referenceWeeks = 8)
        assertSame(ultrasound, profileDatingToSave(ultrasound.copy(referenceDays = 0), ultrasound, ongoing = false))
        assertNull(profileDatingToSave(ultrasound.copy(referenceDays = null), ultrasound, ongoing = false))
    }

    @Test fun newOrChangedDatesStillRequireAnOngoingPregnancy() {
        val original = PregnancyDating(DatingMethod.FUM, lmp = "2024-01-01")
        val changed = original.copy(lmp = "2026-10-01")
        assertNull(profileDatingToSave(changed, original, ongoing = false))
        assertNull(profileDatingToSave(changed, null, ongoing = false))
        assertSame(changed, profileDatingToSave(changed, original, ongoing = true))
        assertNull(profileDatingToSave(original.copy(method = DatingMethod.MEDICAL), original, ongoing = false))
    }
}
