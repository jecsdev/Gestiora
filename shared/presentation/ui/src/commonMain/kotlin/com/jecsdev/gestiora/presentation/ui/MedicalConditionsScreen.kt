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
import com.jecsdev.gestiora.presentation.viewmodels.MedicalConditionViewModel
import kotlinx.coroutines.launch

@Composable
internal fun MedicalConditionsScreen(viewModel: MedicalConditionViewModel, onNavigationLockChange: (Boolean) -> Unit) {
    val ui = LocalUiCopy.current
    val scope = retainedOperationScope()
    var conditions by retain { mutableStateOf<List<MedicalCondition>?>(null) }
    var loadError by retain { mutableStateOf(false) }
    var operationError by retain { mutableStateOf(false) }
    var busy by retain { mutableStateOf(false) }
    var editing by retain { mutableStateOf<MedicalCondition?>(null) }
    var deleting by retain { mutableStateOf<MedicalCondition?>(null) }
    suspend fun reload() {
        val result = viewModel.load()
        conditions = result.getOrNull()
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
            MedicalConditionForm(editing!!, busy, operationError, onCancel = onCancel, onTextChanges = onTextChanges) { condition ->
                busy = true
                scope.launch {
                    try {
                        operationError = false
                        val result = if (condition.id.isEmpty()) viewModel.add(condition) else viewModel.update(condition)
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
        EditorialHeader(ui.s(Res.string.conditions_and_risks), ui.s(Res.string.only_conditions_and_risks_identified_by_a_health), compact = true)
        when {
            loadError -> {
                Text(ui.s(Res.string.we_couldn_t_read_your_conditions_your_data), color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text(ui.s(Res.string.retry)) }
            }
            conditions == null -> CircularProgressIndicator()
            else -> {
                GradientButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = { operationError = false; editing = MedicalCondition("", "", todayIsoDate(), ""); onNavigationLockChange(true) }) { Text(ui.s(Res.string.new_condition)) }
                if (conditions!!.isEmpty()) EditorialEmptyState(ui.s(Res.string.you_haven_t_recorded_any_conditions_yet))
                conditions!!.forEach { condition ->
                    EditorialRecordCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EditorialStatus(condition.status.label(ui))
                            Text(condition.name, style = MaterialTheme.typography.titleMedium)
                            Text(condition.professional)
                            Text(dateLabel(ui, condition.date), style = MaterialTheme.typography.labelMedium)
                            condition.instructions?.let { MedicalConditionDetail(ui.s(Res.string.medical_instructions), it) }
                            condition.treatment?.let { MedicalConditionDetail(ui.s(Res.string.prescribed_treatment), it) }
                            condition.monitoring?.let { MedicalConditionDetail(ui.s(Res.string.monitoring_requirements), it) }
                            condition.followUp?.let { MedicalConditionDetail(ui.s(Res.string.follow_up), dateLabel(ui, it)) }
                            condition.notes?.let { MedicalConditionDetail(ui.s(Res.string.historical_notes), it) }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled = !busy, onClick = { operationError = false; editing = condition; onNavigationLockChange(true) }) { Text(ui.s(Res.string.edit)) }
                                TextButton(enabled = !busy, onClick = { deleting = condition; onNavigationLockChange(true) }) { Text(ui.s(Res.string.delete)) }
                            }
                        }
                    }
                }
                if (operationError) Text(ui.s(Res.string.we_couldn_t_delete_the_condition_try_again), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { condition ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text(ui.s(Res.string.delete_condition)) },
            text = { Text(ui.s(Res.string.this_condition_will_be_deleted_from_this_device)) },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        operationError = false
                        val result = viewModel.delete(condition.id)
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
private fun MedicalConditionDetail(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value)
}


@Composable
private fun MedicalConditionForm(initial: MedicalCondition, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onTextChanges: (Boolean) -> Unit, onSave: (MedicalCondition) -> Unit) {
    val ui = LocalUiCopy.current
    var name by retain(initial) { mutableStateOf(initial.name) }
    var date by retain(initial) { mutableStateOf(initial.date) }
    var professional by retain(initial) { mutableStateOf(initial.professional) }
    var instructions by retain(initial) { mutableStateOf(initial.instructions.orEmpty()) }
    var treatment by retain(initial) { mutableStateOf(initial.treatment.orEmpty()) }
    var monitoring by retain(initial) { mutableStateOf(initial.monitoring.orEmpty()) }
    var followUp by retain(initial) { mutableStateOf(initial.followUp.orEmpty()) }
    var status by retain(initial) { mutableStateOf(initial.status) }
    var notes by retain(initial) { mutableStateOf(initial.notes.orEmpty()) }
    var invalid by retain(initial) { mutableStateOf(false) }
    SideEffect {
        onTextChanges(
            name.hasTextChangedFrom(initial.name) ||
                professional.hasTextChangedFrom(initial.professional) ||
                instructions.hasTextChangedFrom(initial.instructions) ||
                treatment.hasTextChangedFrom(initial.treatment) ||
                monitoring.hasTextChangedFrom(initial.monitoring) ||
                notes.hasTextChangedFrom(initial.notes),
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorialHeader(if (initial.id.isEmpty()) ui.s(Res.string.new_condition) else ui.s(Res.string.edit_condition))
        Text(ui.s(Res.string.only_conditions_and_risks_identified_by_a_health), style = MaterialTheme.typography.bodySmall)
        MedicalConditionTextField(ui.s(Res.string.condition_identified_by_a_professional), name, 512, busy, invalid && name.isBlank()) { name = it }
        DateField(ui.s(Res.string.date_identified), date, enabled = !busy) { date = it }
        MedicalConditionTextField(ui.s(Res.string.health_professional), professional, 512, busy, invalid && professional.isBlank()) { professional = it }
        MedicalConditionTextField(ui.s(Res.string.medical_instructions_optional), instructions, 4096, busy) { instructions = it }
        MedicalConditionTextField(ui.s(Res.string.prescribed_treatment_optional), treatment, 4096, busy) { treatment = it }
        MedicalConditionTextField(ui.s(Res.string.monitoring_requirements_optional), monitoring, 4096, busy) { monitoring = it }
        DateField(ui.s(Res.string.follow_up_date_optional), followUp, clearable = true, enabled = !busy) { followUp = it }
        Text(ui.s(Res.string.this_date_does_not_create_an_appointment_or), style = MaterialTheme.typography.bodySmall)
        Text(ui.s(Res.string.status), style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConditionStatus.entries.forEach { option ->
                FilterChip(status == option, { status = option }, enabled = !busy, label = { Text(option.label(ui)) })
            }
        }
        MedicalConditionTextField(ui.s(Res.string.historical_notes_optional), notes, 4096, busy) { notes = it }
        if (invalid) Text(ui.s(Res.string.enter_the_condition_professional_and_a_valid_date), color = MaterialTheme.colorScheme.error)
        if (saveError) Text(ui.s(Res.string.we_couldn_t_save_the_condition_your_changes), color = MaterialTheme.colorScheme.error)
        GradientButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
            val condition = medicalConditionForSave(initial, name, date, professional, instructions, treatment, monitoring, followUp, status, notes)
            invalid = condition == null
            if (condition != null) onSave(condition)
        }) { Text(if (busy) ui.s(Res.string.saving) else ui.s(Res.string.save_condition)) }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = onCancel) { Text(ui.s(Res.string.cancel)) }
    }
}

@Composable
private fun MedicalConditionTextField(label: String, value: String, limit: Int, busy: Boolean, requiredError: Boolean = false, onChange: (String) -> Unit) {
    PastelTextField(value, onChange, label = { Text(label) }, enabled = !busy, singleLine = limit == 512, isError = requiredError || value.length > limit, supportingText = { Text("${value.length}/$limit") }, modifier = Modifier.fillMaxWidth())
}
