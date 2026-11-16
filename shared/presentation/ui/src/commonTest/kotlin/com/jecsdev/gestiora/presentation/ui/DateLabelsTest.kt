package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*
import kotlin.test.Test
import kotlin.test.assertEquals

class DateLabelsTest {
    private val ui = UiCopy(mapOf(
        Res.string.invalid_date to "Fecha no válida",
        Res.string.date_short to "%1\$s %2\$s %3\$s",
        Res.string.date_long to "%1\$s de %2\$s de %3\$s",
        Res.string.month_short_1 to "ene",
        Res.string.month_short_2 to "feb",
        Res.string.month_short_3 to "mar",
        Res.string.month_short_4 to "abr",
        Res.string.month_short_5 to "may",
        Res.string.month_short_6 to "jun",
        Res.string.month_short_7 to "jul",
        Res.string.month_short_8 to "ago",
        Res.string.month_short_9 to "sep",
        Res.string.month_short_10 to "oct",
        Res.string.month_short_11 to "nov",
        Res.string.month_short_12 to "dic",
        Res.string.date_month_1 to "enero",
        Res.string.date_month_2 to "febrero",
        Res.string.date_month_3 to "marzo",
        Res.string.date_month_4 to "abril",
        Res.string.date_month_5 to "mayo",
        Res.string.date_month_6 to "junio",
        Res.string.date_month_7 to "julio",
        Res.string.date_month_8 to "agosto",
        Res.string.date_month_9 to "septiembre",
        Res.string.date_month_10 to "octubre",
        Res.string.date_month_11 to "noviembre",
        Res.string.date_month_12 to "diciembre"
    ))

    @Test
    fun spanishMonthsMatchPrototypeWithoutChangingCivilDates() {
        val short = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        val full = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
        for (month in 1..12) {
            val iso = "2026-${month.toString().padStart(2, '0')}-01"
            assertEquals("1 ${short[month - 1]} 2026", dateLabel(ui, iso))
            assertEquals("1 de ${full[month - 1]} de 2026", dateLabel(ui, iso, expanded = true))
            assertEquals(iso, utcMillisToIso(requireNotNull(isoToUtcMillis(iso))))
        }
    }

    @Test
    fun leapDaysAndSupportedYearBoundariesRemainExact() {
        assertEquals("29 feb 2000", dateLabel(ui, "2000-02-29"))
        assertEquals("31 dic 1969", dateLabel(ui, "1969-12-31"))
        assertEquals("1 ene 0001", dateLabel(ui, "0001-01-01"))
        assertEquals("31 de diciembre de 9999", dateLabel(ui, "9999-12-31", expanded = true))
    }

    @Test
    fun missingAndInvalidDatesAreDistinctAndNeverNormalized() {
        assertEquals("—", dateLabel(ui, null))
        assertEquals("—", dateLabel(ui, "", expanded = true))
        for (invalid in listOf("2026-02-29", "1900-02-29", "2026-04-31", "0000-01-01", "2026-13-01", "2026-1-01", "2026-11-14T00:00:00Z", " 2026-11-14", "not a date")) {
            assertEquals("Fecha no válida", dateLabel(ui, invalid))
            assertEquals("Fecha no válida", dateLabel(ui, invalid, expanded = true))
        }
    }
}
