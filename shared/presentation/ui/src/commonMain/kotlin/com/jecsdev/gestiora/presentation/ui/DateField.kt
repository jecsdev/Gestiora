package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Stores calendar days as ISO strings. */
@Composable
internal fun DateField(
    label: String,
    value: String,
    maxDate: String? = null,
    clearable: Boolean = false,
    enabled: Boolean = true,
    initialMonth: String? = null,
    onValueChange: (String) -> Unit,
) {
    val ui = LocalUiCopy.current
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { open = true }, enabled = enabled, colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics {
            contentDescription = ui.s(Res.string.date_field_accessibility, label, if (value.isEmpty()) ui.s(Res.string.select_date) else dateLabel(ui, value, expanded = true))
        }) {
            Text(if (value.isEmpty()) ui.s(Res.string.select_date) else dateLabel(ui, value), modifier = Modifier.clearAndSetSemantics { })
        }
        if (clearable && value.isNotEmpty()) {
            TextButton(onClick = { onValueChange("") }, enabled = enabled) { Text(ui.s(Res.string.clear_date, label)) }
        }
    }
    if (open) {
        DateSelectionDialog(
            value = value,
            maxDate = maxDate,
            initialMonth = initialMonth,
            onDismiss = { open = false },
            onConfirm = {
                onValueChange(it)
                open = false
            },
        )
    }
}

@Composable
private fun DateSelectionDialog(
    value: String,
    maxDate: String?,
    initialMonth: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        DateSelectionContent(value, maxDate, initialMonth, todayIsoDate(), onDismiss, onConfirm)
    }
}

@Composable
private fun DateSelectionContent(
    value: String,
    maxDate: String?,
    initialMonth: String?,
    today: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val ui = LocalUiCopy.current
    val maximum = maxDate?.takeIf { isoToUtcMillis(it) != null }
    val startingDate = value.takeIf { isoToUtcMillis(it) != null && (maximum == null || it <= maximum) }
    val startingMonth = startingDate?.take(7)
        ?: initialMonth?.takeIf { isoToUtcMillis("$it-01") != null }
        ?: today.take(7)
    val years = listOfNotNull(startingMonth.take(4).toIntOrNull(), maximum?.take(4)?.toIntOrNull(), today.take(4).toIntOrNull())
    val yearRange = minOf(1900, years.min())..maxOf(2100, years.max())
    var selectedDate by remember { mutableStateOf(startingDate.orEmpty()) }
    var month by remember { mutableStateOf(startingMonth) }
    var choosingYear by remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.Center) {
        CalendarCard(
            modifier = Modifier.safeDrawingPadding().padding(12.dp)
                .widthIn(max = 440.dp).fillMaxWidth(),
        ) {
            EditorialHeader(ui.s(Res.string.select_a_date), compact = true)
            Box(Modifier.weight(1f, fill = false).fillMaxWidth().padding(top = 12.dp)) {
                if (choosingYear) {
                    val currentYear = month.take(4).toInt()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        state = rememberLazyListState(initialFirstVisibleItemIndex = (currentYear - yearRange.first - 2).coerceAtLeast(0)),
                    ) {
                        items(yearRange.last - yearRange.first + 1) { index ->
                            val year = yearRange.first + index
                            TextButton(onClick = {
                                month = year.toString().padStart(4, '0') + month.drop(4)
                                choosingYear = false
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(year.toString())
                            }
                        }
                    }
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        MonthCalendar(
                            month = month,
                            selectedDate = selectedDate,
                            today = today,
                            markedDates = emptySet(),
                            enabled = true,
                            onMonthChange = { month = it },
                            onDateSelect = { selectedDate = it; month = it.take(7) },
                            yearRange = yearRange,
                            maxDate = maximum,
                            onChooseYear = { choosingYear = true },
                        )
                    }
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (choosingYear) {
                    TextButton(onClick = { choosingYear = false }) { Text(ui.s(Res.string.back)) }
                } else {
                    TextButton(onClick = onDismiss) { Text(ui.s(Res.string.cancel)) }
                    GradientButton(
                        onClick = { onConfirm(selectedDate) },
                        enabled = selectedDate.isNotEmpty() && (maximum == null || selectedDate <= maximum),
                    ) { Text(ui.s(Res.string.ok)) }
                }
            }
        }
    }
}

@Preview(name = "Date picker · selected · dark", widthDp = 384, heightDp = 680, locale = "en")
@Composable
private fun DateSelectionDarkPreview() {
    DateSelectionPreview(dark = true, value = "2026-08-19")
}

@Preview(name = "Date picker · six weeks · compact dark", widthDp = 320, heightDp = 560, locale = "es")
@Composable
private fun DateSelectionCompactPreview() {
    DateSelectionPreview(dark = true, value = "")
}

@Composable
private fun DateSelectionPreview(dark: Boolean, value: String) {
    PreviewContent(dark) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DateSelectionContent(value, "2026-08-23", "2026-08", "2026-08-23", {}, {})
        }
    }
}

@Preview(name = "Date field · empty and selected", widthDp = 384, heightDp = 250, locale = "en")
@Composable
private fun DateFieldPreview() {
    PreviewContent {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DateField("Appointment date", "", initialMonth = "2026-08") {}
            DateField("Follow-up date", "2026-08-26", clearable = true) {}
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
