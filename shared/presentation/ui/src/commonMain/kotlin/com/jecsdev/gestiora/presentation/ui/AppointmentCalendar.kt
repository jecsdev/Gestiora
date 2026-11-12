package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private const val DAY_MILLIS = 86_400_000L
private val months = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

internal fun shiftCalendarMonth(month: String, offset: Int): String? {
    if (month.length != 7 || isoToUtcMillis("$month-01") == null) return null
    val index = (month.substring(0, 4).toInt() - 1) * 12L + month.substring(5, 7).toInt() - 1 + offset
    if (index !in 0 until 9999L * 12) return null
    return "${(index / 12 + 1).toString().padStart(4, '0')}-${(index % 12 + 1).toString().padStart(2, '0')}"
}

/** Monday-first weeks, with null cells outside the supported 0001–9999 range. */
internal fun calendarMonthDays(month: String): List<String?> {
    val first = isoToUtcMillis("$month-01") ?: return emptyList()
    val epochDay = first / DAY_MILLIS
    val weekday = (((epochDay + 3) % 7 + 7) % 7).toInt()
    val next = shiftCalendarMonth(month, 1)?.let { isoToUtcMillis("$it-01") }
    val length = if (next == null) 31 else ((next - first) / DAY_MILLIS).toInt()
    val count = ((weekday + length + 6) / 7) * 7
    val min = requireNotNull(isoToUtcMillis("0001-01-01"))
    val max = requireNotNull(isoToUtcMillis("9999-12-31"))
    return (0 until count).map { index ->
        val date = first + (index - weekday) * DAY_MILLIS
        if (date in min..max) utcMillisToIso(date) else null
    }
}

@Composable
internal fun AppointmentCalendar(
    month: String,
    selectedDate: String,
    today: String,
    markedDates: Set<String>,
    enabled: Boolean,
    onMonthChange: (String) -> Unit,
    onDateSelect: (String) -> Unit,
) {
    val previous = shiftCalendarMonth(month, -1)
    val next = shiftCalendarMonth(month, 1)
    val days = calendarMonthDays(month)
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { previous?.let(onMonthChange) }, enabled = enabled && previous != null, modifier = Modifier.semantics { contentDescription = "Mes anterior" }) { Text("‹") }
                Text("${months[month.substring(5, 7).toInt() - 1]} ${month.substring(0, 4)}", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { next?.let(onMonthChange) }, enabled = enabled && next != null, modifier = Modifier.semantics { contentDescription = "Mes siguiente" }) { Text("›") }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(day, style = MaterialTheme.typography.labelSmall) }
                }
            }
            days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        val selected = date != null && date == selectedDate
                        val marked = date != null && date in markedDates
                        val background = when {
                            selected -> MaterialTheme.colorScheme.primary
                            date != null && date == today -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val foreground = when {
                            selected -> MaterialTheme.colorScheme.onPrimary
                            date != null && !date.startsWith(month) -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp).background(background, CircleShape)
                            .then(if (date == null) Modifier else Modifier.semantics { contentDescription = "$date${if (marked) ", tiene citas" else ""}${if (selected) ", seleccionada" else ""}" }.clickable(enabled = enabled) { onDateSelect(date) }),
                            contentAlignment = Alignment.Center) {
                            if (date != null) {
                                Text(date.substring(8).toInt().toString(), color = foreground, style = MaterialTheme.typography.bodySmall)
                                if (marked) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(4.dp).background(if (selected) foreground else MaterialTheme.colorScheme.primary, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}
