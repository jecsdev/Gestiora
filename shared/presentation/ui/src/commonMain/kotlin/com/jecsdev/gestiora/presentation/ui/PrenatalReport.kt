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
import com.jecsdev.gestiora.domain.models.MedicalCondition
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.domain.models.PrenatalNote
import com.jecsdev.gestiora.domain.models.VaccineRecord
import com.jecsdev.gestiora.domain.models.VitalRecord
import com.jecsdev.gestiora.domain.models.VitalType
import com.jecsdev.gestiora.presentation.viewmodels.MedicalConditionViewModel
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalHistoryEntry
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalHistoryUiState
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalHistoryViewModel
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalNoteViewModel
import com.jecsdev.gestiora.presentation.viewmodels.VaccineRecordViewModel
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
    data class Image(val attachment: PrenatalReportAttachment, val caption: String) : PrenatalReportBlock
}

data class PrenatalReportAttachment(
    val kind: ClinicalAttachmentKind,
    val recordId: String,
    val name: String,
    val isImage: Boolean,
    val imageSlot: Int? = null,
)

data class PrenatalReportDocument(
    val filename: String,
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
    val attachments: suspend (ClinicalAttachmentKind, String, Boolean) -> List<PrenatalReportAttachment>,
    val preview: suspend (PrenatalReportDocument) -> List<PrenatalReportPage>,
)

internal val LocalPrenatalReport = staticCompositionLocalOf<PrenatalReportControls?> { null }

private enum class ReportSection(val label: StringResource) {
    Vitals(Res.string.premium_report_measurements),
    Visits(Res.string.consultations),
    Studies(Res.string.examinations),
    Conditions(Res.string.conditions),
    Vaccines(Res.string.vaccines),
    Notes(Res.string.notes_and_tasks),
}

private val monthlySections = listOf(ReportSection.Vitals, ReportSection.Visits, ReportSection.Studies)

private data class ReportData(
    val vitals: List<VitalRecord>,
    val entries: List<ReportEntry>,
    val conditions: List<MedicalCondition>,
    val vaccines: List<VaccineRecord>,
    val notes: List<PrenatalNote>,
) {
    fun hasRecords(section: ReportSection): Boolean = when (section) {
        ReportSection.Vitals -> vitals.isNotEmpty()
        ReportSection.Visits -> entries.any { it.entry is PrenatalHistoryEntry.Visit }
        ReportSection.Studies -> entries.any { it.entry is PrenatalHistoryEntry.Study }
        ReportSection.Conditions -> conditions.isNotEmpty()
        ReportSection.Vaccines -> vaccines.isNotEmpty()
        ReportSection.Notes -> notes.isNotEmpty()
    }
}

private data class ReportEntry(val entry: PrenatalHistoryEntry, val attachments: List<PrenatalReportAttachment>)

private fun VaccineRecord.reportDate(): String? = administered ?: scheduled

private fun <T> List<T>.forReportPeriod(period: String?, date: (T) -> String?): List<T> =
    if (period == null) this else inPrenatalMonth(this, period, date)

@Composable
internal fun PrenatalReportRoute(profile: PregnancyProfile, onBack: () -> Unit, historyOnly: Boolean = false) {
    val controls = LocalPrenatalReport.current ?: return
    val ui = LocalUiCopy.current
    val vitalModel = koinInject<VitalRecordViewModel>()
    val historyModel = koinInject<PrenatalHistoryViewModel>()
    val conditionModel = koinInject<MedicalConditionViewModel>()
    val vaccineModel = koinInject<VaccineRecordViewModel>()
    val noteModel = koinInject<PrenatalNoteViewModel>()
    val todayMonth = todayIsoDate().take(7)
    var month by retain { mutableStateOf(todayMonth) }
    var allDates by retain { mutableStateOf(historyOnly) }
    val period = month.takeUnless { allDates }
    var sections by retain { mutableStateOf((if (historyOnly) ReportSection.entries else monthlySections).toSet()) }
    var font by retain { mutableStateOf(PrenatalReportFont.Serif) }
    var size by retain { mutableIntStateOf(12) }
    var ink by retain { mutableStateOf(PrenatalReportInk.Plum) }
    var weightUnit by retain { mutableStateOf(WeightUnit.KILOGRAMS) }
    var preview by retain { mutableStateOf(false) }
    var exportRequested by retain { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    val result by key(period, attempt) {
        produceState<Result<ReportData>?>(null, historyOnly) {
            value = try {
                val vitals = vitalModel.load().getOrThrow().forReportPeriod(period, VitalRecord::date)
                    .sortedWith(compareBy<VitalRecord> { it.date }.thenBy { it.id })
                val history = historyModel.load() as? PrenatalHistoryUiState.Ready ?: error("Unable to load history")
                val selected = history.entries.forReportPeriod(period, PrenatalHistoryEntry::date)
                val entries = selected.map { entry ->
                    val attachment = when (entry) {
                        is PrenatalHistoryEntry.Visit ->
                            controls.attachments(ClinicalAttachmentKind.Consultation, entry.consultation.id, historyOnly)
                        is PrenatalHistoryEntry.Study ->
                            controls.attachments(ClinicalAttachmentKind.Exam, entry.exam.id, historyOnly)
                    }
                    ReportEntry(entry, attachment)
                }
                val conditions = if (historyOnly) {
                    conditionModel.load().getOrThrow().forReportPeriod(period, MedicalCondition::date)
                        .sortedByDescending(MedicalCondition::date)
                } else emptyList()
                val vaccines = if (historyOnly) {
                    vaccineModel.load().getOrThrow().forReportPeriod(period, VaccineRecord::reportDate)
                        .sortedByDescending(VaccineRecord::reportDate)
                } else emptyList()
                val notes = if (historyOnly) {
                    noteModel.load().getOrThrow().forReportPeriod(period, PrenatalNote::followUp)
                        .sortedByDescending(PrenatalNote::followUp)
                } else emptyList()
                Result.success(ReportData(vitals, entries, conditions, vaccines, notes))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Result.failure(failure)
            }
        }
    }
    val availableSections = if (historyOnly) {
        result?.getOrNull()?.let { data -> ReportSection.entries.filter(data::hasRecords) }.orEmpty()
    } else monthlySections
    val includedSections = sections.intersect(availableSections.toSet())
    val document = remember(result, ui, profile, period, includedSections, font, size, ink, weightUnit, historyOnly) {
        result?.getOrNull()?.let { data ->
            val name = if (historyOnly) "Gestiora-prenatal-records" else "Gestiora"
            val suffix = period?.let { "-$it" }.orEmpty()
            PrenatalReportDocument(
                "$name$suffix.pdf", font, size, ink,
                reportBlocks(ui, period, profile, data, includedSections, weightUnit, historyOnly),
            )
        }
    }
    val closePreview: () -> Unit = { preview = false }
    AppointmentBackHandler(enabled = preview, onBack = closePreview)
    var previewAttempt by remember { mutableIntStateOf(0) }
    ScreenExchange(preview) { showingPreview ->
        val pages by key(showingPreview, document, previewAttempt) {
            produceState<Result<List<PrenatalReportPage>>?>(null) {
                if (showingPreview && document != null) {
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
            state = key(showingPreview) { rememberLazyListState() },
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ScreenBackButton(if (showingPreview) closePreview else onBack) }
            item {
                val title = when {
                    showingPreview -> Res.string.premium_report_preview
                    historyOnly -> Res.string.premium_history_report_title
                    else -> Res.string.premium_reports
                }
                EditorialHeader(ui.s(title))
            }
            if (showingPreview) {
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
                        if (historyOnly) {
                            FilterChip(allDates, onClick = { allDates = !allDates }, label = { Text(ui.s(Res.string.premium_all_dates)) })
                        }
                        if (!allDates) PrenatalMonthPicker(month, todayMonth) { month = it }
                        if (availableSections.isNotEmpty()) {
                            Text(ui.s(Res.string.premium_report_sections), style = MaterialTheme.typography.titleMedium)
                            ChoiceChips(availableSections, { it in includedSections }, { ui.s(it.label) }) {
                                sections = if (it in sections) sections - it else sections + it
                            }
                        } else if (result?.isSuccess == true) {
                            Text(ui.s(Res.string.premium_period_empty))
                        }
                        if (ReportSection.Vitals in includedSections) WeightUnitSelector(weightUnit) { weightUnit = it }
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
                            enabled = document != null && includedSections.isNotEmpty(),
                        ) { Text(ui.s(Res.string.premium_report_preview)) }
                    }
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
    month: String?,
    profile: PregnancyProfile,
    data: ReportData,
    sections: Set<ReportSection>,
    unit: WeightUnit,
    historyOnly: Boolean,
): List<PrenatalReportBlock> = buildList {
    fun heading(label: String, level: Int = 1) { add(PrenatalReportBlock.Text(label, level)) }
    fun paragraph(lines: List<String?>) {
        val content = lines.filterNotNull().filter { it.isNotBlank() }.joinToString("\n")
        if (content.isNotEmpty()) add(PrenatalReportBlock.Text(content))
    }
    fun field(label: StringResource, value: String?): String? =
        value?.takeIf { it.isNotBlank() }?.let { ui.s(Res.string.premium_report_field, ui.s(label), it) }

    heading(ui.s(if (historyOnly) Res.string.premium_history_report_title else Res.string.premium_report_title), 2)
    val periodLabel = month?.let { monthLabel(ui, it) } ?: ui.s(Res.string.premium_all_dates)
    paragraph(listOf(periodLabel, profile.motherName, ui.s(Res.string.premium_report_source)))
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
                for ((entry, files) in entries) {
                    val date = entry.date?.let { dateLabel(ui, it) } ?: ui.s(Res.string.no_date_recorded)
                    val title = ui.s(Res.string.premium_report_dated, date, when (entry) {
                        is PrenatalHistoryEntry.Visit -> ui.s(Res.string.premium_history_visit)
                        is PrenatalHistoryEntry.Study -> entry.exam.name
                    })
                    val lines = when (entry) {
                        is PrenatalHistoryEntry.Visit -> with(entry.consultation) {
                            listOf(
                                title,
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
                                title, status.label(ui),
                                kind.label(ui).takeIf { historyOnly },
                                field(Res.string.request_date, requested?.takeIf { historyOnly }?.let { dateLabel(ui, it) }),
                                field(Res.string.scheduled_date, scheduled?.takeIf { historyOnly }?.let { dateLabel(ui, it) }),
                                field(Res.string.recorded_completion_date, completed?.takeIf { historyOnly }?.let { dateLabel(ui, it) }),
                                field(Res.string.requesting_doctor, physician),
                                field(Res.string.results_summary, results), field(Res.string.medical_observations, notes),
                                field(Res.string.attachments_reference, attachment),
                            )
                        }
                    }
                    val fileNames = files.filterNot { historyOnly && it.isImage }.map { it.name }
                    paragraph(lines + fileNames.map { field(Res.string.premium_report_attachment, it) })
                    if (historyOnly) files.filter { it.isImage }.forEach { image ->
                        add(PrenatalReportBlock.Image(image, "$title\n${image.name}"))
                    }
                }
            }
            ReportSection.Conditions -> for (record in data.conditions) {
                paragraph(listOf(
                    ui.s(Res.string.premium_report_dated, dateLabel(ui, record.date), record.name),
                    record.status.label(ui),
                    field(Res.string.health_professional, record.professional),
                    field(Res.string.medical_instructions, record.instructions),
                    field(Res.string.prescribed_treatment, record.treatment),
                    field(Res.string.monitoring_requirements, record.monitoring),
                    field(Res.string.follow_up, record.followUp?.let { dateLabel(ui, it) }),
                    field(Res.string.historical_notes, record.notes),
                ))
            }
            ReportSection.Vaccines -> for (record in data.vaccines) {
                paragraph(listOf(
                    record.name,
                    record.status.label(ui),
                    field(Res.string.dose, record.dose),
                    field(Res.string.scheduled_date, record.scheduled?.let { dateLabel(ui, it) }),
                    field(Res.string.recorded_administration_date, record.administered?.let { dateLabel(ui, it) }),
                    field(Res.string.professional_provider, record.provider),
                    field(Res.string.location, record.location),
                    field(Res.string.proof_reference, record.proof),
                    field(Res.string.notes, record.notes),
                ))
            }
            ReportSection.Notes -> for (record in data.notes) {
                paragraph(listOf(
                    record.title,
                    record.category.label(ui),
                    record.status.label(ui),
                    record.body,
                    field(Res.string.follow_up, record.followUp?.let { dateLabel(ui, it) }),
                ))
            }
        }
    }
}
