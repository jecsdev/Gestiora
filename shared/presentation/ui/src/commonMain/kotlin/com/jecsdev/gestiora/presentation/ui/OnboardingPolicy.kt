package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.DatingMethod
import com.jecsdev.gestiora.domain.models.PregnancyDating

internal enum class OnboardingBackAction { BLOCK, DISMISS_DISCARD, PREVIOUS_STEP, CONFIRM_DISCARD, SYSTEM }

internal fun onboardingBackAction(step: Int, editingProfile: Boolean, saving: Boolean, discardOpen: Boolean): OnboardingBackAction = when {
    saving -> OnboardingBackAction.BLOCK
    discardOpen -> OnboardingBackAction.DISMISS_DISCARD
    step > 1 -> OnboardingBackAction.PREVIOUS_STEP
    editingProfile -> OnboardingBackAction.CONFIRM_DISCARD
    step == 1 -> OnboardingBackAction.PREVIOUS_STEP
    else -> OnboardingBackAction.SYSTEM
}

/** Personal edits must not force a change to historical pregnancy dates. */
internal fun profileDatingToSave(draft: PregnancyDating, original: PregnancyDating?, ongoing: Boolean): PregnancyDating? {
    val unchanged = original != null && draft.method == original.method && when (draft.method) {
        DatingMethod.FUM -> draft.lmp == original.lmp?.takeUnless(String::isBlank) &&
            draft.dueDate == original.dueDate?.takeUnless(String::isBlank)
        else -> draft.referenceDate == original.referenceDate && draft.referenceWeeks == original.referenceWeeks &&
            draft.referenceDays == (original.referenceDays ?: 0)
    }
    return when {
        unchanged -> original
        ongoing -> draft
        else -> null
    }
}
