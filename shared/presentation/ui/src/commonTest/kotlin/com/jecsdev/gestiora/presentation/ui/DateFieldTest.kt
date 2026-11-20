package com.jecsdev.gestiora.presentation.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateFieldTest {
    @Test
    fun utcDatesRoundTripWithoutTimezoneShifts() {
        for (date in listOf("1900-01-01", "1969-12-31", "1970-01-01", "2000-02-29", "2026-11-11", "2100-03-01")) {
            assertEquals(date, utcMillisToIso(requireNotNull(isoToUtcMillis(date))))
        }
        assertEquals(-86_400_000L, isoToUtcMillis("1969-12-31"))
        assertEquals("1969-12-31", utcMillisToIso(-1))
    }

    @Test
    fun invalidDatesAreRejected() {
        for (date in listOf("", "2026-2-01", "2026-02-29", "1900-02-29", "2026-13-01", "0000-01-01", "aaaa-bb-cc")) {
            assertNull(isoToUtcMillis(date))
        }
    }
}
