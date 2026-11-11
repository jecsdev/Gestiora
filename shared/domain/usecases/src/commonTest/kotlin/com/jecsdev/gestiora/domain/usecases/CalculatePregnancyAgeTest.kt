package com.jecsdev.gestiora.domain.usecases

import com.jecsdev.gestiora.domain.models.DatingMethod
import com.jecsdev.gestiora.domain.models.PregnancyDating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalculatePregnancyAgeTest {
    private val calculate = CalculatePregnancyAge()

    @Test fun fumProduces280DayDueDate() {
        val age = calculate(PregnancyDating(DatingMethod.FUM, lmp = "2026-01-01"), "2026-02-12")!!
        assertEquals(6, age.weeks)
        assertEquals(0, age.days)
        assertEquals("2026-10-08", age.dueDate)
        assertEquals(238, age.daysLeft)
    }

    @Test fun ultrasoundReferenceSupersedesFum() {
        val age = calculate(
            PregnancyDating(DatingMethod.ULTRASOUND, lmp = "2026-01-01", referenceDate = "2026-03-01", referenceWeeks = 10),
            "2026-03-01",
        )!!
        assertEquals(10, age.weeks)
        assertEquals(0, age.days)
        assertEquals("2026-09-27", age.dueDate)
    }

    @Test fun dueDateFallbackAndLeapDayUseCalendarDays() {
        val age = calculate(PregnancyDating(DatingMethod.FUM, dueDate = "2024-11-24"), "2024-02-29")!!
        assertEquals(11, age.totalDays)
        assertEquals("2024-11-24", age.dueDate)
    }

    @Test fun medicalReferenceAndTrimesterBoundary() {
        val dating = PregnancyDating(DatingMethod.MEDICAL, referenceDate = "2026-04-09", referenceWeeks = 13, referenceDays = 6)
        assertEquals(1, calculate(dating, "2026-04-09")!!.trimester)
        assertEquals(2, calculate(dating, "2026-04-10")!!.trimester)
    }

    @Test fun invalidAndOutOfRangeInputsAreRejected() {
        assertNull(calculate(PregnancyDating(DatingMethod.FUM, lmp = "2026-02-30"), "2026-03-01"))
        assertNull(calculate(PregnancyDating(DatingMethod.ULTRASOUND, referenceDate = "2026-03-01", referenceWeeks = 43), "2026-03-01"))
        assertNull(calculate(PregnancyDating(DatingMethod.FUM, lmp = "2026-01-01"), "bad-date"))
        assertFalse(calculate.isOngoing(calculate(PregnancyDating(DatingMethod.FUM, lmp = "2026-03-02"), "2026-03-01")))
        assertTrue(calculate.isOngoing(calculate(PregnancyDating(DatingMethod.FUM, lmp = "2026-01-01"), "2026-02-12")))
    }
}
