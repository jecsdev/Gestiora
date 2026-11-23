package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.*
import com.jecsdev.gestiora.presentation.viewmodels.ExamRecordViewModel
import kotlinx.coroutines.launch

@Composable
internal fun ExamRecordsScreen(
    viewModel: ExamRecordViewModel,
    onNavigationLockChange: (Boolean) -> Unit,
    onPremiumPlans: () -> Unit,
    onHistory: (() -> Unit)?,
) {
    val ui = LocalUiCopy.current
    val scope = retainedOperationScope()
    var exams by retain { mutableStateOf<List<ExamRecord>?>(null) }
    var loadError by retain { mutableStateOf(false) }
    var operationError by retain { mutableStateOf(false) }
    var busy by retain { mutableStateOf(false) }
    var editing by retain { mutableStateOf<ExamRecord?>(null) }
    var deleting by retain { mutableStateOf<ExamRecord?>(null) }
    suspend fun reload() {
        val result = viewModel.load()
        exams = result.getOrNull()
        loadError = result.isFailure
    }
    LaunchedEffect(viewModel) { reload() }
    AppointmentBackHandler(busy || deleting != null) {
        when {
            busy -> Unit
            deleting != null -> { deleting = null; onNavigationLockChange(false) }
        }
    }
    if (editing != null) {
        RecordEditorSheet(
            busy = busy,
            onClose = {
                editing = null
                operationError = false
                onNavigationLockChange(false)
            },
        ) { onCancel, onTextChanges ->
            ExamRecordForm(editing!!, busy, operationError, onCancel = onCancel, onTextChanges = onTextChanges) { exam ->
                busy = true
                scope.launch {
                    try {
                        operationError = false
                        val result = if (exam.id.isEmpty()) viewModel.add(exam) else viewModel.update(exam)
                        if (result.isSuccess) {
                            editing = null
                            reload()
                        } else operationError = true
                    } finally { busy = false; if (editing == null) onNavigationLockChange(false) }
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        EditorialHeader(ui.s(Res.string.examinations), ui.s(Res.string.your_examinations_and_results_are_saved_on_this), compact = true)
        when {
            loadError -> {
                Text(ui.s(Res.string.we_couldn_t_read_your_examinations_your_data), color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text(ui.s(Res.string.retry)) }
            }
            exams == null -> CircularProgressIndicator()
            else -> {
                GradientButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = { operationError = false; editing = ExamRecord("", "", requested = todayIsoDate()); onNavigationLockChange(true) }) { Text(ui.s(Res.string.new_examination)) }
                if (exams!!.isEmpty()) EditorialEmptyState(ui.s(Res.string.you_haven_t_recorded_any_examinations_yet))
                exams!!.forEach { exam ->
                    EditorialRecordCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EditorialStatus(exam.status.label(ui))
                            Text(exam.name, style = MaterialTheme.typography.titleMedium)
                            ExamRecordDetails(exam)
                            ClinicalAttachmentActions(ClinicalAttachmentKind.Exam, exam.id, !busy)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled = !busy, onClick = { operationError = false; editing = exam; onNavigationLockChange(true) }) { Text(ui.s(Res.string.edit)) }
                                TextButton(enabled = !busy, onClick = { deleting = exam; onNavigationLockChange(true) }) { Text(ui.s(Res.string.delete)) }
                            }
                        }
                    }
                }
                if (operationError) Text(ui.s(Res.string.we_couldn_t_delete_the_examination_try_again), color = MaterialTheme.colorScheme.error)
                if (!busy && editing == null && deleting == null) {
                    PremiumRecordsBanner(onPlans = onPremiumPlans, onHistory = onHistory)
                }
            }
        }
    }
    deleting?.let { exam ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text(ui.s(Res.string.delete_examination)) },
            text = { Text(ui.s(Res.string.this_examination_will_be_deleted_from_this_device)) },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        operationError = false
                        val result = viewModel.delete(exam.id)
                        deleting = null
                        if (result.isSuccess) reload() else operationError = true
                    } finally { busy = false; onNavigationLockChange(false) }
                }
            }) { Text(ui.s(Res.string.delete)) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null; onNavigationLockChange(false) }) { Text(ui.s(Res.string.cancel)) } },
        )
    }
}




@Composable
internal fun ExamRecordDetails(exam: ExamRecord) {
    val ui = LocalUiCopy.current
    Text(exam.kind.label(ui), style = MaterialTheme.typography.labelMedium)
    val referenceDate = exam.completed ?: exam.scheduled ?: exam.requested
    ExamRecordDetail(ui.s(Res.string.reference_date), referenceDate?.let { dateLabel(ui, it) } ?: ui.s(Res.string.no_date_recorded))
    exam.requested?.let { ExamRecordDetail(ui.s(Res.string.request_date), dateLabel(ui, it)) }
    exam.scheduled?.let { ExamRecordDetail(ui.s(Res.string.scheduled_date), dateLabel(ui, it)) }
    exam.completed?.let { ExamRecordDetail(ui.s(Res.string.recorded_completion_date), dateLabel(ui, it)) }
    exam.physician?.let { ExamRecordDetail(ui.s(Res.string.requesting_doctor), it) }
    exam.results?.let { ExamRecordDetail(ui.s(Res.string.results_summary), it) }
    exam.notes?.let { ExamRecordDetail(ui.s(Res.string.medical_observations), it) }
    exam.attachment?.let { ExamRecordDetail(ui.s(Res.string.document_or_image_reference), it) }
}

@Composable
private fun ExamRecordDetail(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value)
}

@Composable
private fun ExamRecordForm(initial: ExamRecord, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onTextChanges: (Boolean) -> Unit, onSave: (ExamRecord) -> Unit) {
    val ui = LocalUiCopy.current
    var name by retain(initial) { mutableStateOf(initial.name) }
    var kind by retain(initial) { mutableStateOf(initial.kind) }
    var requested by retain(initial) { mutableStateOf(initial.requested.orEmpty()) }
    var scheduled by retain(initial) { mutableStateOf(initial.scheduled.orEmpty()) }
    var completed by retain(initial) { mutableStateOf(initial.completed.orEmpty()) }
    var status by retain(initial) { mutableStateOf(initial.status) }
    var physician by retain(initial) { mutableStateOf(initial.physician.orEmpty()) }
    var results by retain(initial) { mutableStateOf(initial.results.orEmpty()) }
    var notes by retain(initial) { mutableStateOf(initial.notes.orEmpty()) }
    var attachment by retain(initial) { mutableStateOf(initial.attachment.orEmpty()) }
    var reminder by retain(initial) { mutableStateOf(initial.reminder) }
    var invalid by retain(initial) { mutableStateOf(false) }
    var kindMenu by retain(initial) { mutableStateOf(false) }
    SideEffect {
        onTextChanges(
            name.hasTextChangedFrom(initial.name) ||
                physician.hasTextChangedFrom(initial.physician) ||
                results.hasTextChangedFrom(initial.results) ||
                notes.hasTextChangedFrom(initial.notes) ||
                attachment.hasTextChangedFrom(initial.attachment),
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorialHeader(if (initial.id.isEmpty()) ui.s(Res.string.new_examination) else ui.s(Res.string.edit_examination))
        ExamRecordTextField(ui.s(Res.string.examination_name), name, 512, busy, invalid && name.isBlank()) { name = it }
        Text(ui.s(Res.string.type), style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(enabled = !busy, onClick = { kindMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(kind.label(ui)) }
            DropdownMenu(expanded = kindMenu, onDismissRequest = { kindMenu = false }) {
                ExamKind.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label(ui)) }, onClick = { kind = option; kindMenu = false }) }
            }
        }
        DateField(ui.s(Res.string.request_date_optional), requested, clearable = true, enabled = !busy) { requested = it }
        DateField(ui.s(Res.string.scheduled_date_optional), scheduled, clearable = true, enabled = !busy) { scheduled = it }
        Text(ui.s(Res.string.status), style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExamStatus.entries.forEach { option -> FilterChip(status == option, { status = option }, enabled = !busy, label = { Text(option.label(ui)) }) }
        }
        DateField(if (status == ExamStatus.COMPLETED) ui.s(Res.string.completion_date) else ui.s(Res.string.recorded_completion_date_optional), completed, clearable = true, enabled = !busy) { completed = it }
        Text(ui.s(Res.string.changing_status_preserves_the_recorded_completion_date_you), style = MaterialTheme.typography.bodySmall)
        ExamRecordTextField(ui.s(Res.string.requesting_doctor_optional), physician, 512, busy) { physician = it }
        ExamRecordTextField(ui.s(Res.string.results_summary_optional), results, 4096, busy) { results = it }
        ExamRecordTextField(ui.s(Res.string.medical_observations_optional), notes, 4096, busy) { notes = it }
        ExamRecordTextField(ui.s(Res.string.document_or_image_reference_optional), attachment, 512, busy) { attachment = it }
        Text(ui.s(Res.string.the_reference_is_text_files_are_not_attached), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = reminder, onCheckedChange = { reminder = it }, enabled = !busy)
            Text(ui.s(Res.string.exam_reminder_option), modifier = Modifier.weight(1f))
        }
        Text(ui.s(Res.string.exam_reminder_detail), style = MaterialTheme.typography.bodySmall)
        ReminderNotificationSettings()
        if (invalid) Text(ui.s(Res.string.enter_the_examination_and_if_completed_its_completion), color = MaterialTheme.colorScheme.error)
        if (saveError) Text(ui.s(Res.string.we_couldn_t_save_the_examination_your_changes), color = MaterialTheme.colorScheme.error)
        GradientButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
            val exam = examRecordForSave(initial, name, kind, requested, scheduled, completed, status, physician, results, notes, attachment)
            invalid = exam == null
            if (exam != null) onSave(exam.copy(reminder = reminder))
        }) { Text(if (busy) ui.s(Res.string.saving) else ui.s(Res.string.save_examination)) }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = onCancel) { Text(ui.s(Res.string.cancel)) }
    }
}

@Composable
private fun ExamRecordTextField(label: String, value: String, limit: Int, busy: Boolean, requiredError: Boolean = false, onChange: (String) -> Unit) {
    PastelTextField(value, onChange, label = { Text(label) }, enabled = !busy, singleLine = limit == 512, isError = requiredError || value.length > limit, supportingText = { Text("${value.length}/$limit") }, modifier = Modifier.fillMaxWidth())
}
