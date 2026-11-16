package com.jecsdev.gestiora.presentation.ui

private val shortSpanishMonths = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private val spanishMonths = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")

/** Presentation only: civil dates remain ISO in models, filters, ordering and picker callbacks. */
internal fun dateLabel(value: String?, expanded: Boolean = false): String {
    if (value.isNullOrEmpty()) return "—"
    if (isoToUtcMillis(value) == null) return "Fecha no válida"
    val day = value.substring(8).toInt()
    val month = value.substring(5, 7).toInt() - 1
    val year = value.substring(0, 4)
    return if (expanded) "$day de ${spanishMonths[month]} de $year"
    else "$day ${shortSpanishMonths[month]} $year"
}
