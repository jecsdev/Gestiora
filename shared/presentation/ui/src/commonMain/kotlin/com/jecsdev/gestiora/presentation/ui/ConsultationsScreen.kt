package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.*
import com.jecsdev.gestiora.presentation.viewmodels.ConsultationViewModel
import kotlinx.coroutines.launch

@Composable
internal fun ConsultationsScreen(viewModel: ConsultationViewModel, onNavigationLockChange: (Boolean) -> Unit) {
    val ui = LocalUiCopy.current
    val scope = retainedOperationScope()
    var consultations by retain { mutableStateOf<List<Consultation>?>(null) }
    var loadError by retain { mutableStateOf(false) }
    var operationError by retain { mutableStateOf(false) }
    var busy by retain { mutableStateOf(false) }
    var editing by retain { mutableStateOf<Consultation?>(null) }
    var deleting by retain { mutableStateOf<Consultation?>(null) }
    suspend fun reload() {
        val result = viewModel.load()
        consultations = result.getOrNull()
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
            ConsultationForm(editing!!, busy, operationError, onCancel = onCancel, onTextChanges = onTextChanges) { consultation ->
                busy = true
                scope.launch {
                    try {
                        operationError = false
                        val result = if (consultation.id.isEmpty()) viewModel.add(consultation) else viewModel.update(consultation)
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
        EditorialHeader(ui.s(Res.string.prenatal_consultations), ui.s(Res.string.your_history_is_saved_on_this_device_the), compact = true)
        when {
            loadError -> {
                Text(ui.s(Res.string.we_couldn_t_read_your_consultations_your_data), color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text(ui.s(Res.string.retry)) }
            }
            consultations == null -> CircularProgressIndicator()
            else -> {
                GradientButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = { operationError = false; editing = Consultation("", todayIsoDate()); onNavigationLockChange(true) }) { Text(ui.s(Res.string.new_consultation)) }
                if (consultations!!.isEmpty()) EditorialEmptyState(ui.s(Res.string.you_haven_t_recorded_any_consultations_yet))
                consultations!!.sortedWith(compareByDescending<Consultation> { it.date }.thenBy { it.id }).forEach { consultation ->
                    EditorialRecordCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(consultation.doctor ?: ui.s(Res.string.prenatal_consultation), style = MaterialTheme.typography.titleMedium)
                            Text(dateLabel(ui, consultation.date), style = MaterialTheme.typography.labelLarge)
                            ConsultationRecordDetails(consultation)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled = !busy, onClick = { operationError = false; editing = consultation; onNavigationLockChange(true) }) { Text(ui.s(Res.string.edit)) }
                                TextButton(enabled = !busy, onClick = { deleting = consultation; onNavigationLockChange(true) }) { Text(ui.s(Res.string.delete)) }
                            }
                        }
                    }
                }
                if (operationError) Text(ui.s(Res.string.we_couldn_t_delete_the_consultation_try_again), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { consultation ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text(ui.s(Res.string.delete_consultation)) },
            text = { Text(ui.s(Res.string.this_consultation_will_be_deleted_from_this_device)) },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        operationError = false
                        val result = viewModel.delete(consultation.id)
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
internal fun ConsultationRecordDetails(consultation: Consultation) {
    val ui = LocalUiCopy.current
    consultation.gestationalAge?.let { ConsultationDetail(ui.s(Res.string.recorded_gestational_age), it) }
    consultation.observations?.let { ConsultationDetail(ui.s(Res.string.observations), it) }
    consultation.instructions?.let { ConsultationDetail(ui.s(Res.string.instructions), it) }
    consultation.medications?.let { ConsultationDetail(ui.s(Res.string.medications), it) }
    consultation.exams?.let { ConsultationDetail(ui.s(Res.string.examinations), it) }
    consultation.followUp?.let { ConsultationDetail(ui.s(Res.string.follow_up), it) }
    consultation.nextDate?.let { ConsultationDetail(ui.s(Res.string.next_appointment), dateLabel(ui, it)) }
    consultation.attachments?.let { ConsultationDetail(ui.s(Res.string.attachments_reference), it) }
}

@Composable
private fun ConsultationDetail(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value)
}

@Composable
private fun ConsultationForm(initial: Consultation, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onTextChanges: (Boolean) -> Unit, onSave: (Consultation) -> Unit) {
    val ui = LocalUiCopy.current
    var date by retain(initial) { mutableStateOf(initial.date) }
    var ga by retain(initial) { mutableStateOf(initial.gestationalAge.orEmpty()) }
    var doctor by retain(initial) { mutableStateOf(initial.doctor.orEmpty()) }
    var observations by retain(initial) { mutableStateOf(initial.observations.orEmpty()) }
    var instructions by retain(initial) { mutableStateOf(initial.instructions.orEmpty()) }
    var medications by retain(initial) { mutableStateOf(initial.medications.orEmpty()) }
    var exams by retain(initial) { mutableStateOf(initial.exams.orEmpty()) }
    var followUp by retain(initial) { mutableStateOf(initial.followUp.orEmpty()) }
    var nextDate by retain(initial) { mutableStateOf(initial.nextDate.orEmpty()) }
    var attachments by retain(initial) { mutableStateOf(initial.attachments.orEmpty()) }
    var invalid by retain(initial) { mutableStateOf(false) }
    SideEffect {
        onTextChanges(
            ga.hasTextChangedFrom(initial.gestationalAge) ||
                doctor.hasTextChangedFrom(initial.doctor) ||
                observations.hasTextChangedFrom(initial.observations) ||
                instructions.hasTextChangedFrom(initial.instructions) ||
                medications.hasTextChangedFrom(initial.medications) ||
                exams.hasTextChangedFrom(initial.exams) ||
                followUp.hasTextChangedFrom(initial.followUp) ||
                attachments.hasTextChangedFrom(initial.attachments),
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorialHeader(if (initial.id.isEmpty()) ui.s(Res.string.new_consultation) else ui.s(Res.string.edit_consultation))
        DateField(ui.s(Res.string.consultation_date), date, enabled = !busy) { date = it }
        ConsultationTextField(ui.s(Res.string.recorded_gestational_age_optional), ga, 512, busy) { ga = it }
        Text(ui.s(Res.string.copy_the_gestational_age_stated_at_that_consultation), style = MaterialTheme.typography.bodySmall)
        ConsultationTextField(ui.s(Res.string.doctor_optional), doctor, 512, busy) { doctor = it }
        ConsultationTextField(ui.s(Res.string.doctor_s_observations_optional), observations, 4096, busy) { observations = it }
        ConsultationTextField(ui.s(Res.string.medical_instructions_optional), instructions, 4096, busy) { instructions = it }
        ConsultationTextField(ui.s(Res.string.prescribed_medications_optional), medications, 4096, busy) { medications = it }
        ConsultationTextField(ui.s(Res.string.requested_examinations_optional), exams, 4096, busy) { exams = it }
        ConsultationTextField(ui.s(Res.string.follow_up_requirements_optional), followUp, 4096, busy) { followUp = it }
        DateField(ui.s(Res.string.next_appointment_date_optional), nextDate, clearable = true, enabled = !busy) { nextDate = it }
        Text(ui.s(Res.string.this_date_does_not_create_a_calendar_appointment), style = MaterialTheme.typography.bodySmall)
        ConsultationTextField(ui.s(Res.string.attachments_reference_optional), attachments, 512, busy) { attachments = it }
        Text(ui.s(Res.string.save_a_reference_to_the_document_such_as), style = MaterialTheme.typography.bodySmall)
        if (invalid) Text(ui.s(Res.string.select_a_valid_date_and_respect_the_field), color = MaterialTheme.colorScheme.error)
        if (saveError) Text(ui.s(Res.string.we_couldn_t_save_the_consultation_your_changes), color = MaterialTheme.colorScheme.error)
        GradientButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
            invalid = isoToUtcMillis(date) == null || (nextDate.isNotEmpty() && isoToUtcMillis(nextDate) == null) || ga.length > 512 || doctor.length > 512 || attachments.length > 512 || listOf(observations, instructions, medications, exams, followUp).any { it.length > 4096 }
            fun optional(value: String) = value.trim().ifEmpty { null }
            if (!invalid) onSave(initial.copy(date = date, gestationalAge = optional(ga), doctor = optional(doctor), observations = optional(observations), instructions = optional(instructions), medications = optional(medications), exams = optional(exams), followUp = optional(followUp), nextDate = nextDate.ifEmpty { null }, attachments = optional(attachments)))
        }) { Text(if (busy) ui.s(Res.string.saving) else ui.s(Res.string.save_consultation)) }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = onCancel) { Text(ui.s(Res.string.cancel)) }
    }
}

@Composable
private fun ConsultationTextField(label: String, value: String, limit: Int, busy: Boolean, onChange: (String) -> Unit) {
    PastelTextField(value, onChange, label = { Text(label) }, enabled = !busy, singleLine = limit == 512, isError = value.length > limit, supportingText = { Text("${value.length}/$limit") }, modifier = Modifier.fillMaxWidth())
}
