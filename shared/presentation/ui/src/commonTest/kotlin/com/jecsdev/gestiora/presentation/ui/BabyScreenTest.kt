package com.jecsdev.gestiora.presentation.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class BabyScreenTest {
    @Test
    fun stagesFollowReferenceBoundaries() {
        val boundaries = listOf(8, 12, 16, 20, 24, 28, 32, 36)
        boundaries.forEachIndexed { index, boundary ->
            val previous = if (index == 0) 0 else boundaries[index - 1]
            assertEquals(previous, babyStage(boundary - 1).from)
            assertEquals(boundary, babyStage(boundary).from)
        }
        assertEquals(0, babyStage(-1).from)
        assertEquals(36, babyStage(100).from)
    }

    @Test
    fun artworkUsesClosestEarlierReference() {
        assertEquals(4, babyReferenceWeek(0))
        assertEquals(4, babyReferenceWeek(7))
        assertEquals(8, babyReferenceWeek(8))
        assertEquals(20, babyReferenceWeek(27))
        assertEquals(28, babyReferenceWeek(35))
        assertEquals(36, babyReferenceWeek(40))
    }
}
