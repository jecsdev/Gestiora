package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.ConditionStatus
import com.jecsdev.gestiora.domain.models.MedicalCondition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MedicalFollowUpTest {
    @Test fun excludesResolvedAndPreservesRecordedOrderWithoutRankingUrgency() {
        val resolved = MedicalCondition("resolved", "Registrada", "2026-11-12", "Profesional", status = ConditionStatus.RESOLVED)
        val followUp = resolved.copy(id = "follow", status = ConditionStatus.FOLLOW_UP)
        val active = resolved.copy(id = "active", date = "2026-11-01", status = ConditionStatus.ACTIVE)
        assertEquals(followUp, firstMedicalFollowUp(listOf(resolved, followUp, active)))
        assertEquals(active, firstMedicalFollowUp(listOf(active, followUp, resolved)))
        assertNull(firstMedicalFollowUp(listOf(resolved)))
        assertNull(firstMedicalFollowUp(emptyList()))
    }
}
