package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private const val DAY_MILLIS = 86_400_000L
private val months = listOf(Res.string.january, Res.string.february, Res.string.march, Res.string.april, Res.string.may, Res.string.june, Res.string.july, Res.string.august, Res.string.september, Res.string.october, Res.string.november, Res.string.december)

internal fun newCalendarEventDate(month: String, selectedDate: String, today: String): String =
    selectedDate.takeIf { it.take(7) == month }
        ?: today.takeIf { it.take(7) == month }.orEmpty()

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
internal fun MonthCalendar(
    month: String,
    selectedDate: String,
    today: String,
    markedDates: Set<String>,
    enabled: Boolean,
    onMonthChange: (String) -> Unit,
    onDateSelect: (String) -> Unit,
    markedDescription: String? = null,
    yearRange: IntRange = 1..9999,
    maxDate: String? = null,
    onChooseYear: (() -> Unit)? = null,
) {
    val ui = LocalUiCopy.current
    val markedLabel = markedDescription ?: ui.s(Res.string.has_appointments)
    val previous = shiftCalendarMonth(month, -1)?.takeIf { it.take(4).toInt() in yearRange }
    val next = shiftCalendarMonth(month, 1)?.takeIf { it.take(4).toInt() in yearRange }
    val days = calendarMonthDays(month)
    val monthTitle = ui.s(Res.string.calendar_month_title, ui.s(months[month.substring(5, 7).toInt() - 1]), month.substring(0, 4))
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { previous?.let(onMonthChange) }, enabled = enabled && previous != null, modifier = Modifier.semantics { contentDescription = ui.s(Res.string.previous_month) }) { Text("‹") }
                if (onChooseYear == null) {
                    Text(monthTitle, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                } else {
                    TextButton(onClick = onChooseYear, modifier = Modifier.weight(1f), enabled = enabled,
                        contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("$monthTitle ▾", style = MaterialTheme.typography.titleMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                TextButton(onClick = { next?.let(onMonthChange) }, enabled = enabled && next != null, modifier = Modifier.semantics { contentDescription = ui.s(Res.string.next_month) }) { Text("›") }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf(ui.s(Res.string.monday), ui.s(Res.string.tuesday), ui.s(Res.string.wednesday), ui.s(Res.string.thursday), ui.s(Res.string.friday), ui.s(Res.string.saturday), ui.s(Res.string.sunday)).forEachIndexed { index, day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(ui.s(Res.string.weekday_initials).split("|")[index], style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.clearAndSetSemantics { contentDescription = day })
                    }
                }
            }
            days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        val isSelected = date != null && date == selectedDate
                        val marked = date != null && date in markedDates
                        val selectable = date != null && date.take(4).toInt() in yearRange && (maxDate == null || date <= maxDate)
                        val background = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            date != null && date == today -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val foreground = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            !selectable -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            !date.startsWith(month) -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Box(Modifier.weight(1f).heightIn(min = 48.dp).padding(2.dp)
                            .then(if (isSelected) Modifier.background(accentGradient(), CircleShape) else Modifier.background(background, CircleShape))
                            .then(if (date == null) Modifier else Modifier.semantics {
                                contentDescription = ui.s(Res.string.calendar_day_accessibility, dateLabel(ui, date, expanded = true), if (date == today) ", " + ui.s(Res.string.today) else "", if (marked) ", $markedLabel" else "")
                                selected = isSelected
                            }.clickable(enabled = enabled && selectable, role = Role.Button) { onDateSelect(date) }),
                            contentAlignment = Alignment.Center) {
                            if (date != null) {
                                Text(date.substring(8).toInt().toString(), color = foreground, style = MaterialTheme.typography.bodySmall, modifier = Modifier.clearAndSetSemantics { })
                                if (marked) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(4.dp).background(if (isSelected) foreground else MaterialTheme.colorScheme.primary, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}
