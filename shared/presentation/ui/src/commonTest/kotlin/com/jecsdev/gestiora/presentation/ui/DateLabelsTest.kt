package com.jecsdev.gestiora.presentation.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class DateLabelsTest {
    @Test
    fun spanishMonthsMatchPrototypeWithoutChangingCivilDates() {
        val short = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        val full = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
        for (month in 1..12) {
            val iso = "2026-${month.toString().padStart(2, '0')}-01"
            assertEquals("1 ${short[month - 1]} 2026", dateLabel(iso))
            assertEquals("1 de ${full[month - 1]} de 2026", dateLabel(iso, expanded = true))
            assertEquals(iso, utcMillisToIso(requireNotNull(isoToUtcMillis(iso))))
        }
    }

    @Test
    fun leapDaysAndSupportedYearBoundariesRemainExact() {
        assertEquals("29 feb 2000", dateLabel("2000-02-29"))
        assertEquals("31 dic 1969", dateLabel("1969-12-31"))
        assertEquals("1 ene 0001", dateLabel("0001-01-01"))
        assertEquals("31 de diciembre de 9999", dateLabel("9999-12-31", expanded = true))
    }

    @Test
    fun missingAndInvalidDatesAreDistinctAndNeverNormalized() {
        assertEquals("—", dateLabel(null))
        assertEquals("—", dateLabel("", expanded = true))
        for (invalid in listOf("2026-02-29", "1900-02-29", "2026-04-31", "0000-01-01", "2026-13-01", "2026-1-01", "2026-11-14T00:00:00Z", " 2026-11-14", "not a date")) {
            assertEquals("Fecha no válida", dateLabel(invalid))
            assertEquals("Fecha no válida", dateLabel(invalid, expanded = true))
        }
    }
}
