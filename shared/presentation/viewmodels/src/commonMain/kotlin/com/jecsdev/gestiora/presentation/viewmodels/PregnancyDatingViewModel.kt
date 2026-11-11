package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.PregnancyAge
import com.jecsdev.gestiora.domain.models.PregnancyDating
import com.jecsdev.gestiora.domain.usecases.CalculatePregnancyAge

class PregnancyDatingViewModel(private val calculate: CalculatePregnancyAge) {
    fun preview(dating: PregnancyDating, at: String): PregnancyAge? =
        calculate(dating, at)?.takeIf(calculate::isOngoing)
}
