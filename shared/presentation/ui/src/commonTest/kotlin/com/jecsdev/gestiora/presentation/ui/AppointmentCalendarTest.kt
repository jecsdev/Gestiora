package com.jecsdev.gestiora.presentation.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppointmentCalendarTest {
    @Test
    fun monthGridStartsMondayAndIncludesAdjacentDays() {
        val days = calendarMonthDays("2024-02")
        assertEquals(35, days.size)
        assertEquals("2024-01-29", days.first())
        assertEquals("2024-02-29", days[31])
        assertEquals("2024-03-03", days.last())
        assertEquals("2021-02-01", calendarMonthDays("2021-02").first())
        assertEquals(28, calendarMonthDays("2021-02").size)
    }

    @Test
    fun monthNavigationAndGridRespectSupportedRange() {
        assertEquals("2025-01", shiftCalendarMonth("2024-12", 1))
        assertEquals("2023-12", shiftCalendarMonth("2024-01", -1))
        assertNull(shiftCalendarMonth("0001-01", -1))
        assertNull(shiftCalendarMonth("9999-12", 1))
        assertNull(shiftCalendarMonth("2024-99", 1))
        assertEquals("0001-01-01", calendarMonthDays("0001-01").first())
        assertEquals("9999-12-31", calendarMonthDays("9999-12").filterNotNull().last())
        assertEquals(emptyList(), calendarMonthDays("bad"))
    }
}
