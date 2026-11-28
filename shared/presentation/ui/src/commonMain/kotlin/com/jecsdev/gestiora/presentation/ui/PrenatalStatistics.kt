package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.VitalRecord
import com.jecsdev.gestiora.domain.models.VitalType
import com.jecsdev.gestiora.presentation.viewmodels.VitalRecordViewModel
import gestiora.shared.presentation.ui.generated.resources.*
import org.koin.compose.koinInject
import kotlin.math.ceil
import kotlin.math.floor

data class PrenatalChartPoint(val x: Float, val y: Float)

data class PrenatalChart(
    val series: List<List<PrenatalChartPoint>>,
    val lower: String,
    val upper: String,
    val firstDate: String,
    val lastDate: String?,
)

internal fun <T> inPrenatalMonth(entries: List<T>, month: String, date: (T) -> String?): List<T> =
    entries.filter { entry -> date(entry)?.let { it.startsWith("$month-") && isoToUtcMillis(it) != null } == true }

internal fun prenatalChart(
    sortedRecords: List<VitalRecord>,
    type: VitalType,
    unit: WeightUnit,
    dateLabel: (String) -> String,
): PrenatalChart? {
    val records = sortedRecords.filter { it.type == type && it.value?.isFinite() == true }
    if (records.isEmpty()) return null
    val primary = records.map { record ->
        val value = requireNotNull(record.value)
        if (type == VitalType.WEIGHT) weightInUnit(value, unit) else value
    }
    val secondary = records.map { it.value2?.takeIf(Double::isFinite) }
    val values = primary + secondary.filterNotNull()
    val min = floor(values.min())
    val max = maxOf(ceil(values.max()), min + 1.0)
    val dates = records.map { requireNotNull(isoToUtcMillis(it.date)) }
    val duration = dates.last() - dates.first()
    fun point(index: Int, value: Double): PrenatalChartPoint = PrenatalChartPoint(
        x = if (duration == 0L) 0.5f else ((dates[index] - dates.first()).toDouble() / duration).toFloat(),
        y = ((value - min) / (max - min)).toFloat(),
    )
    return PrenatalChart(
        series = listOf(primary.mapIndexed(::point), secondary.mapIndexedNotNull { index, value ->
            value?.let { point(index, it) }
        }).filter { it.isNotEmpty() },
        lower = formatVitalNumber(min),
        upper = formatVitalNumber(max),
        firstDate = dateLabel(records.first().date),
        lastDate = records.last().date.takeIf { duration != 0L }?.let(dateLabel),
    )
}

@Composable
internal fun PrenatalStatisticsRoute(onBack: () -> Unit) {
    val ui = LocalUiCopy.current
    val viewModel = koinInject<VitalRecordViewModel>()
    var attempt by remember { mutableIntStateOf(0) }
    val loaded by produceState<Result<List<VitalRecord>>?>(null, viewModel, attempt) {
        value = null
        value = viewModel.load()
    }
    val todayMonth = todayIsoDate().take(7)
    var month by retain { mutableStateOf(todayMonth) }
    var allMonths by retain { mutableStateOf(false) }
    var type by retain { mutableStateOf(VitalType.WEIGHT) }
    var unit by retain { mutableStateOf(WeightUnit.KILOGRAMS) }
    val records = remember(loaded, type, month, allMonths) {
        val source = loaded?.getOrNull().orEmpty().filter { it.type == type && isoToUtcMillis(it.date) != null }
        (if (allMonths) source else inPrenatalMonth(source, month, VitalRecord::date))
            .sortedWith(compareBy<VitalRecord> { it.date }.thenBy { it.id })
    }
    val chart = remember(records, type, unit, ui) { prenatalChart(records, type, unit) { dateLabel(ui, it) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenBackButton(onBack) }
        item { EditorialHeader(ui.s(Res.string.premium_statistics), ui.s(Res.string.premium_statistics_detail)) }
        item {
            EditorialRecordCard {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(VitalType.WEIGHT, VitalType.BLOOD_PRESSURE, VitalType.FUNDAL_HEIGHT).forEach { option ->
                        FilterChip(type == option, onClick = { type = option }, label = { Text(option.label(ui)) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!allMonths, onClick = { allMonths = false }, label = { Text(ui.s(Res.string.premium_by_month)) })
                    FilterChip(allMonths, onClick = { allMonths = true }, label = { Text(ui.s(Res.string.premium_all_dates)) })
                }
                if (!allMonths) PrenatalMonthPicker(month, todayMonth) { month = it }
                if (type == VitalType.WEIGHT) WeightUnitSelector(unit) { unit = it }
            }
        }
        when {
            loaded == null -> item { CircularProgressIndicator() }
            loaded?.isFailure == true -> item { PrenatalLoadError { attempt++ } }
            chart == null -> item { EditorialEmptyState(ui.s(Res.string.premium_period_empty)) }
            else -> {
                item {
                    EditorialRecordCard {
                        val primary = MaterialTheme.colorScheme.primary
                        val secondary = MaterialTheme.colorScheme.tertiary
                        PrenatalChartPlot(chart, listOf(primary, secondary))
                        if (type == VitalType.BLOOD_PRESSURE) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(ui.s(Res.string.systolic_mmhg), color = primary, style = MaterialTheme.typography.labelMedium)
                                Text(ui.s(Res.string.diastolic_mmhg), color = secondary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                items(records.asReversed(), key = { it.id }) { record ->
                    EditorialRecordCard {
                        Text(dateLabel(ui, record.date), style = MaterialTheme.typography.labelMedium)
                        Text(vitalRecordValue(record, ui, unit), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PrenatalMonthPicker(month: String, latest: String, onChange: (String) -> Unit) {
    val ui = LocalUiCopy.current
    val previous = shiftCalendarMonth(month, -1)
    val next = shiftCalendarMonth(month, 1)?.takeIf { it <= latest }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MonthNavigationButton(ui.s(Res.string.previous_month), previous != null, pointsRight = false) {
            previous?.let(onChange)
        }
        Text(monthLabel(ui, month), Modifier.weight(1f), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge)
        MonthNavigationButton(ui.s(Res.string.next_month), next != null, pointsRight = true) { next?.let(onChange) }
    }
}

@Composable
internal fun PrenatalLoadError(onRetry: () -> Unit) {
    val ui = LocalUiCopy.current
    EditorialRecordCard {
        Text(ui.s(Res.string.premium_load_error), color = MaterialTheme.colorScheme.error)
        GradientButton(onClick = onRetry) { Text(ui.s(Res.string.retry)) }
    }
}

@Composable
internal fun PrenatalChartPlot(
    chart: PrenatalChart,
    colors: List<Color>,
    labelStyle: TextStyle = MaterialTheme.typography.labelSmall,
    grid: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.height(112.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(chart.upper, style = labelStyle)
                Text(chart.lower, style = labelStyle)
            }
            Canvas(Modifier.weight(1f).height(112.dp)) {
                val inset = 4.dp.toPx()
                val height = size.height - 2 * inset
                fun position(point: PrenatalChartPoint) =
                    Offset(inset + point.x * (size.width - 2 * inset), inset + (1 - point.y) * height)
                for (fraction in listOf(0f, 0.5f, 1f)) {
                    val y = inset + height * fraction
                    drawLine(grid, Offset(inset, y), Offset(size.width - inset, y), 1.dp.toPx())
                }
                chart.series.forEachIndexed { index, points ->
                    val color = colors[index]
                    points.zipWithNext { start, end ->
                        if (start.x != end.x) drawLine(color, position(start), position(end), 2.dp.toPx())
                    }
                    points.forEach { point -> drawCircle(color, 3.dp.toPx(), position(point)) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(chart.firstDate, Modifier.weight(1f), style = labelStyle)
            chart.lastDate?.let { Text(it, Modifier.weight(1f), style = labelStyle, textAlign = TextAlign.End) }
        }
    }
}
