package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.domain.models.VitalRecord
import com.jecsdev.gestiora.domain.models.VitalType
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalHistoryEntry
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalHistoryUiState
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalHistoryViewModel
import com.jecsdev.gestiora.presentation.viewmodels.VitalRecordViewModel
import gestiora.shared.presentation.ui.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject
import kotlin.coroutines.cancellation.CancellationException

enum class PrenatalReportFont(val androidFamily: String) { Serif("serif"), Sans("sans-serif") }
enum class PrenatalReportInk(val accent: Long) {
    Plum(0xFF633861), Rose(0xFF943F60), Monochrome(0xFF303039),
}
enum class PrenatalReportExport { Idle, Saving, Saved, Failed }

sealed interface PrenatalReportBlock {
    data class Text(val value: String, val level: Int = 0) : PrenatalReportBlock
    data class Chart(val plot: PrenatalChart) : PrenatalReportBlock
}

data class PrenatalReportDocument(
    val month: String,
    val font: PrenatalReportFont,
    val size: Int,
    val ink: PrenatalReportInk,
    val blocks: List<PrenatalReportBlock>,
)

class PrenatalReportPage(
    val aspectRatio: Float,
    val description: String,
    val draw: DrawScope.() -> Unit,
)

class PrenatalReportControls(
    val status: PrenatalReportExport,
    val export: (PrenatalReportDocument) -> Unit,
    val attachmentName: suspend (ClinicalAttachmentKind, String) -> String?,
    val preview: suspend (PrenatalReportDocument) -> List<PrenatalReportPage>,
)

internal val LocalPrenatalReport = staticCompositionLocalOf<PrenatalReportControls?> { null }

private enum class ReportSection(val label: StringResource) {
    Vitals(Res.string.premium_report_measurements),
    Visits(Res.string.consultations),
    Studies(Res.string.examinations),
}

private data class ReportMonthData(
    val vitals: List<VitalRecord>,
    val entries: List<ReportEntry>,
)

private data class ReportEntry(val entry: PrenatalHistoryEntry, val attachmentName: String?)

@Composable
internal fun PrenatalReportRoute(profile: PregnancyProfile, onBack: () -> Unit) {
    val controls = LocalPrenatalReport.current ?: return
    val ui = LocalUiCopy.current
    val vitalModel = koinInject<VitalRecordViewModel>()
    val historyModel = koinInject<PrenatalHistoryViewModel>()
    val todayMonth = todayIsoDate().take(7)
    var month by retain { mutableStateOf(todayMonth) }
    var sections by retain { mutableStateOf(ReportSection.entries.toSet()) }
    var font by retain { mutableStateOf(PrenatalReportFont.Serif) }
    var size by retain { mutableIntStateOf(12) }
    var ink by retain { mutableStateOf(PrenatalReportInk.Plum) }
    var weightUnit by retain { mutableStateOf(WeightUnit.KILOGRAMS) }
    var preview by retain { mutableStateOf(false) }
    var exportRequested by retain { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    val result by key(month, attempt) {
        produceState<Result<ReportMonthData>?>(null, vitalModel, historyModel) {
            value = try {
                val vitals = inPrenatalMonth(vitalModel.load().getOrThrow(), month, VitalRecord::date)
                    .sortedWith(compareBy<VitalRecord> { it.date }.thenBy { it.id })
                val history = historyModel.load() as? PrenatalHistoryUiState.Ready ?: error("Unable to load history")
                val entries = inPrenatalMonth(history.entries, month, PrenatalHistoryEntry::date).map { entry ->
                    val attachment = when (entry) {
                        is PrenatalHistoryEntry.Visit -> controls.attachmentName(ClinicalAttachmentKind.Consultation, entry.consultation.id)
                        is PrenatalHistoryEntry.Study -> controls.attachmentName(ClinicalAttachmentKind.Exam, entry.exam.id)
                    }
                    ReportEntry(entry, attachment)
                }
                Result.success(ReportMonthData(vitals, entries))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Result.failure(failure)
            }
        }
    }
    val document = remember(result, ui, profile, month, sections, font, size, ink, weightUnit) {
        result?.getOrNull()?.let { data ->
            PrenatalReportDocument(month, font, size, ink, reportBlocks(ui, month, profile, data, sections, weightUnit))
        }
    }
    val closePreview: () -> Unit = { preview = false }
    AppointmentBackHandler(enabled = preview, onBack = closePreview)
    var previewAttempt by remember { mutableIntStateOf(0) }
    val pages by key(preview, document, previewAttempt) {
        produceState<Result<List<PrenatalReportPage>>?>(null) {
            if (preview && document != null) {
                value = try {
                    Result.success(controls.preview(document))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Result.failure(failure)
                }
            }
        }
    }
    LazyColumn(
        state = key(preview) { rememberLazyListState() },
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenBackButton(if (preview) closePreview else onBack) }
        item { EditorialHeader(ui.s(if (preview) Res.string.premium_report_preview else Res.string.premium_reports)) }
        if (preview) {
            when {
                result?.isFailure == true -> item { PrenatalLoadError { attempt++ } }
                pages == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                pages?.isFailure == true -> item { PrenatalLoadError { previewAttempt++ } }
            }
            if (document != null && pages?.isSuccess == true) {
                item {
                    GradientButton(
                        onClick = { exportRequested = true; controls.export(document) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = controls.status != PrenatalReportExport.Saving,
                    ) { Text(ui.s(Res.string.premium_report_export)) }
                    Text(ui.s(Res.string.premium_report_export_detail), style = MaterialTheme.typography.bodySmall)
                    if (exportRequested) when (controls.status) {
                        PrenatalReportExport.Idle -> Unit
                        PrenatalReportExport.Saving -> LinearProgressIndicator(Modifier.fillMaxWidth())
                        PrenatalReportExport.Saved -> Text(ui.s(Res.string.premium_report_saved))
                        PrenatalReportExport.Failed -> Text(ui.s(Res.string.premium_report_failed), color = MaterialTheme.colorScheme.error)
                    }
                }
                items(pages?.getOrThrow().orEmpty()) { page ->
                    Canvas(
                        modifier = Modifier.fillMaxWidth()
                            .aspectRatio(page.aspectRatio)
                            .shadow(2.dp)
                            .semantics { contentDescription = page.description },
                        onDraw = page.draw,
                    )
                }
            }
        } else {
            item {
                EditorialRecordCard {
                    PrenatalMonthPicker(month, todayMonth) { month = it }
                    Text(ui.s(Res.string.premium_report_sections), style = MaterialTheme.typography.titleMedium)
                    ChoiceChips(ReportSection.entries, { it in sections }, { ui.s(it.label) }) {
                        sections = if (it in sections) sections - it else sections + it
                    }
                    if (ReportSection.Vitals in sections) WeightUnitSelector(weightUnit) { weightUnit = it }
                }
            }
            item {
                EditorialRecordCard {
                    Text(ui.s(Res.string.premium_report_font), style = MaterialTheme.typography.titleMedium)
                    ChoiceChips(PrenatalReportFont.entries, { it == font }, {
                        ui.s(if (it == PrenatalReportFont.Serif) Res.string.premium_report_serif else Res.string.premium_report_sans)
                    }) { font = it }
                    Text(ui.s(Res.string.premium_report_size), style = MaterialTheme.typography.titleMedium)
                    ChoiceChips(listOf(10, 12, 14), { it == size }, { ui.s(Res.string.premium_report_points, it) }) { size = it }
                    Text(ui.s(Res.string.premium_report_color), style = MaterialTheme.typography.titleMedium)
                    ChoiceChips(PrenatalReportInk.entries, { it == ink }, {
                        ui.s(when (it) {
                            PrenatalReportInk.Plum -> Res.string.premium_report_plum
                            PrenatalReportInk.Rose -> Res.string.premium_report_rose
                            PrenatalReportInk.Monochrome -> Res.string.premium_report_mono
                        })
                    }) { ink = it }
                }
            }
            item {
                when {
                    result == null -> CircularProgressIndicator()
                    result?.isFailure == true -> PrenatalLoadError { attempt++ }
                    else -> GradientButton(
                        onClick = { exportRequested = false; preview = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = document != null && sections.isNotEmpty(),
                    ) { Text(ui.s(Res.string.premium_report_preview)) }
                }
            }
        }
    }
}

@Composable
private fun <T> ChoiceChips(options: List<T>, selected: (T) -> Boolean, label: (T) -> String, choose: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected(option), onClick = { choose(option) }, label = { Text(label(option)) })
        }
    }
}

private fun reportBlocks(
    ui: UiCopy,
    month: String,
    profile: PregnancyProfile,
    data: ReportMonthData,
    sections: Set<ReportSection>,
    unit: WeightUnit,
): List<PrenatalReportBlock> = buildList {
    fun heading(label: String, level: Int = 1) { add(PrenatalReportBlock.Text(label, level)) }
    fun paragraph(lines: List<String?>) {
        val content = lines.filterNotNull().filter { it.isNotBlank() }.joinToString("\n")
        if (content.isNotEmpty()) add(PrenatalReportBlock.Text(content))
    }
    fun field(label: StringResource, value: String?): String? =
        value?.takeIf { it.isNotBlank() }?.let { ui.s(Res.string.premium_report_field, ui.s(label), it) }

    heading(ui.s(Res.string.premium_report_title), 2)
    paragraph(listOf(monthLabel(ui, month), profile.motherName, ui.s(Res.string.premium_report_source)))
    for (section in sections.sortedBy { it.ordinal }) {
        heading(ui.s(section.label))
        when (section) {
            ReportSection.Vitals -> {
                val chart = prenatalChart(data.vitals, VitalType.WEIGHT, unit) { dateLabel(ui, it) }
                if (chart != null) {
                    val unitLabel = if (unit == WeightUnit.KILOGRAMS) Res.string.weight_unit_kg else Res.string.weight_unit_lb
                    paragraph(listOf(ui.s(Res.string.premium_report_field, VitalType.WEIGHT.label(ui), ui.s(unitLabel))))
                    add(PrenatalReportBlock.Chart(chart))
                }
                if (data.vitals.isEmpty()) paragraph(listOf(ui.s(Res.string.premium_period_empty)))
                for (record in data.vitals) {
                    paragraph(listOf(
                        ui.s(Res.string.premium_report_dated, dateLabel(ui, record.date), record.label ?: record.type.label(ui)),
                        vitalRecordValue(record, ui, unit),
                        field(Res.string.notes, record.notes),
                    ))
                }
            }
            ReportSection.Visits, ReportSection.Studies -> {
                val entries = data.entries.filter {
                    (it.entry is PrenatalHistoryEntry.Visit) == (section == ReportSection.Visits)
                }
                if (entries.isEmpty()) paragraph(listOf(ui.s(Res.string.premium_period_empty)))
                for ((entry, attachmentName) in entries) {
                    val date = dateLabel(ui, entry.date)
                    val lines = when (entry) {
                        is PrenatalHistoryEntry.Visit -> with(entry.consultation) {
                            listOf(
                                ui.s(Res.string.premium_report_dated, date, ui.s(Res.string.premium_history_visit)),
                                field(Res.string.doctor, doctor), field(Res.string.recorded_gestational_age, gestationalAge),
                                field(Res.string.observations, observations), field(Res.string.instructions, instructions),
                                field(Res.string.medications, medications), field(Res.string.examinations, exams),
                                field(Res.string.follow_up, followUp),
                                field(Res.string.next_appointment, nextDate?.let { dateLabel(ui, it) }),
                                field(Res.string.attachments_reference, attachments),
                            )
                        }
                        is PrenatalHistoryEntry.Study -> with(entry.exam) {
                            listOf(
                                ui.s(Res.string.premium_report_dated, date, name), status.label(ui),
                                field(Res.string.requesting_doctor, physician),
                                field(Res.string.results_summary, results), field(Res.string.medical_observations, notes),
                                field(Res.string.attachments_reference, attachment),
                            )
                        }
                    }
                    paragraph(lines + field(Res.string.premium_report_attachment, attachmentName))
                }
            }
        }
    }
}
