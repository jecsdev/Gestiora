package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.VitalRecord
import com.jecsdev.gestiora.domain.models.VitalType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PrenatalStatisticsTest {
    @Test
    fun monthIncludesItsCivilBoundariesAndExcludesInvalidDates() {
        val dates = listOf("2024-01-31", "2024-02-01", "2024-02-29", "2024-02-30", "2024-03-01", null)
        assertEquals(listOf("2024-02-01", "2024-02-29"), inPrenatalMonth(dates, "2024-02") { it })
    }

    @Test
    fun weightUsesElapsedDaysAndConvertsTheAxisToPounds() {
        val records = listOf(
            VitalRecord("first", "2026-11-02", VitalType.WEIGHT, 64.8),
            VitalRecord("second", "2026-11-09", VitalType.WEIGHT, 65.2),
            VitalRecord("third", "2026-11-23", VitalType.WEIGHT, 66.4),
        )
        val kilograms = assertNotNull(prenatalChart(records, VitalType.WEIGHT, WeightUnit.KILOGRAMS) { it })
        val pounds = assertNotNull(prenatalChart(records, VitalType.WEIGHT, WeightUnit.POUNDS) { it })

        assertEquals(listOf(0f, 1f / 3, 1f), kilograms.series.single().map { it.x })
        assertEquals("64", kilograms.lower)
        assertEquals("67", kilograms.upper)
        assertEquals("142", pounds.lower)
        assertEquals("147", pounds.upper)
        assertNull(prenatalChart(records, VitalType.FUNDAL_HEIGHT, WeightUnit.KILOGRAMS) { it })
    }

    @Test
    fun aSingleBloodPressureReadingHasTwoPointsOnTheSameScale() {
        val records = listOf(VitalRecord("bp", "2026-11-23", VitalType.BLOOD_PRESSURE, 112.0, 72.0))
        val chart = assertNotNull(prenatalChart(records, VitalType.BLOOD_PRESSURE, WeightUnit.KILOGRAMS) { it })
        assertEquals(listOf(PrenatalChartPoint(0.5f, 1f), PrenatalChartPoint(0.5f, 0f)), chart.series.flatten())
        assertNull(chart.lastDate)
    }
}
