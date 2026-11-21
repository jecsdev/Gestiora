package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
internal fun CalendarCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            content = content,
        )
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
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MonthNavigationButton(ui.s(Res.string.previous_month), enabled && previous != null, pointsRight = false) {
                previous?.let(onMonthChange)
            }
            if (onChooseYear == null) {
                Text(monthTitle, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            } else {
                TextButton(onClick = onChooseYear, modifier = Modifier.weight(1f), enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("$monthTitle ▾", style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center)
                }
            }
            MonthNavigationButton(ui.s(Res.string.next_month), enabled && next != null, pointsRight = true) {
                next?.let(onMonthChange)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(ui.s(Res.string.monday), ui.s(Res.string.tuesday), ui.s(Res.string.wednesday), ui.s(Res.string.thursday), ui.s(Res.string.friday), ui.s(Res.string.saturday), ui.s(Res.string.sunday)).forEachIndexed { index, day ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(ui.s(Res.string.weekday_initials).split("|")[index], style = MaterialTheme.typography.labelMedium,
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
                    val background = if (date != null && date == today) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else Color.Transparent
                    val foreground = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        !selectable -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        !date.startsWith(month) -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    val interactionSource = remember(date) { MutableInteractionSource() }
                    Box(Modifier.weight(1f).heightIn(min = 48.dp).padding(2.dp)
                        .then(if (date == null) Modifier else Modifier.semantics {
                            contentDescription = ui.s(Res.string.calendar_day_accessibility, dateLabel(ui, date, expanded = true), if (date == today) ", " + ui.s(Res.string.today) else "", if (marked) ", $markedLabel" else "")
                            selected = isSelected
                        }.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled && selectable,
                            role = Role.Button,
                        ) { onDateSelect(date) }),
                        contentAlignment = Alignment.Center) {
                        if (date != null) {
                            Box(
                                modifier = Modifier.widthIn(max = 44.dp).fillMaxWidth().aspectRatio(1f)
                                    .clip(CircleShape)
                                    .then(if (isSelected) Modifier.background(accentGradient()) else Modifier.background(background))
                                    .indication(interactionSource, ripple()),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(date.substring(8).toInt().toString(), color = foreground, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.clearAndSetSemantics { })
                                if (marked) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(4.dp).background(if (isSelected) foreground else MaterialTheme.colorScheme.primary, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthNavigationButton(description: String, enabled: Boolean, pointsRight: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).semantics { contentDescription = description },
    ) {
        val color = LocalContentColor.current
        Canvas(Modifier.size(24.dp)) {
            val direction = if (pointsRight) 1 else -1
            val halfWidth = 4.dp.toPx() * direction
            val halfHeight = 7.dp.toPx()
            val chevron = Path().apply {
                moveTo(center.x - halfWidth, center.y - halfHeight)
                lineTo(center.x + halfWidth, center.y)
                lineTo(center.x - halfWidth, center.y + halfHeight)
            }
            drawPath(chevron, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Preview(name = "Calendar · selected day and appointments", widthDp = 384, heightDp = 460, locale = "en")
@Composable
private fun MonthCalendarPreview() {
    PreviewContent {
        CalendarCard(Modifier.padding(20.dp)) {
            MonthCalendar(
                month = "2026-08",
                selectedDate = "2026-08-19",
                today = "2026-08-23",
                markedDates = setOf("2026-08-19", "2026-08-26"),
                enabled = true,
                onMonthChange = {},
                onDateSelect = {},
            )
        }
    }
}
