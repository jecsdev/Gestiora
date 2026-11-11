package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Stores calendar days as ISO strings; picker milliseconds are always UTC midnight. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(
    label: String,
    value: String,
    maxDate: String? = null,
    clearable: Boolean = false,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(value.ifEmpty { "Seleccionar fecha" })
        }
        if (clearable && value.isNotEmpty()) {
            TextButton(onClick = { onValueChange("") }, enabled = enabled) { Text("Borrar fecha: $label") }
        }
    }
    if (open) {
        val maximum = isoToUtcMillis(maxDate)
        val selected = isoToUtcMillis(value)?.takeIf { maximum == null || it <= maximum }
        val years = listOfNotNull(value.take(4).toIntOrNull(), maxDate?.take(4)?.toIntOrNull(), todayIsoDate().take(4).toIntOrNull())
            .filter { it in 1..9999 }
        val yearRange = minOf(1900, years.minOrNull() ?: 1900)..maxOf(2100, years.maxOrNull() ?: 2100)
        val selectableDates = remember(maximum) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = maximum == null || utcTimeMillis <= maximum
            }
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selected,
            yearRange = yearRange,
            selectableDates = selectableDates,
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onValueChange(utcMillisToIso(it)) }
                    open = false
                }, enabled = state.selectedDateMillis != null) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = state, title = { Text("Selecciona una fecha") }, showModeToggle = false)
        }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L
private const val UNIX_EPOCH_JDN = 2_440_588L

internal fun isoToUtcMillis(value: String?): Long? {
    if (value == null || value.length != 10 || value[4] != '-' || value[7] != '-' ||
        value.indices.any { it != 4 && it != 7 && value[it] !in '0'..'9' }) return null
    val year = value.substring(0, 4).toInt()
    val month = value.substring(5, 7).toInt()
    val day = value.substring(8, 10).toInt()
    if (year !in 1..9999 || month !in 1..12) return null
    val maxDay = when (month) {
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 31
    }
    if (day !in 1..maxDay) return null
    val a = (14 - month) / 12
    val y = year + 4800 - a
    val m = month + 12 * a - 3
    val jdn = day + (153 * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045L
    return (jdn - UNIX_EPOCH_JDN) * MILLIS_PER_DAY
}

internal fun utcMillisToIso(millis: Long): String {
    val epochDay = millis / MILLIS_PER_DAY - if (millis % MILLIS_PER_DAY < 0) 1 else 0
    val a = epochDay + UNIX_EPOCH_JDN + 32044
    val b = (4 * a + 3) / 146097
    val c = a - (146097 * b) / 4
    val d = (4 * c + 3) / 1461
    val e = c - (1461 * d) / 4
    val m = (5 * e + 2) / 153
    val day = e - (153 * m + 2) / 5 + 1
    val month = m + 3 - 12 * (m / 10)
    val year = 100 * b + d - 4800 + m / 10
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}
