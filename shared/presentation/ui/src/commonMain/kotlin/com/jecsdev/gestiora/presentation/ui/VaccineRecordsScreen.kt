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
import com.jecsdev.gestiora.presentation.viewmodels.VaccineRecordViewModel
import kotlinx.coroutines.launch

@Composable
internal fun VaccineRecordsScreen(viewModel: VaccineRecordViewModel, onNavigationLockChange: (Boolean) -> Unit) {
    val ui = LocalUiCopy.current
    val scope = retainedOperationScope()
    var vaccines by retain { mutableStateOf<List<VaccineRecord>?>(null) }
    var loadError by retain { mutableStateOf(false) }
    var operationError by retain { mutableStateOf(false) }
    var busy by retain { mutableStateOf(false) }
    var editing by retain { mutableStateOf<VaccineRecord?>(null) }
    var deleting by retain { mutableStateOf<VaccineRecord?>(null) }
    var discard by retain { mutableStateOf(false) }
    suspend fun reload() {
        val result = viewModel.load()
        vaccines = result.getOrNull()
        loadError = result.isFailure
    }
    LaunchedEffect(viewModel) { reload() }
    AppointmentBackHandler(busy || editing != null || deleting != null) {
        when {
            busy -> Unit
            discard -> discard = false
            deleting != null -> { deleting = null; onNavigationLockChange(false) }
            editing != null -> discard = true
        }
    }
    if (discard) AlertDialog(
        onDismissRequest = { discard = false },
        title = { Text(ui.s(Res.string.discard_changes)) },
        text = { Text(ui.s(Res.string.changes_you_haven_t_saved_will_be_lost)) },
        confirmButton = { TextButton(onClick = { discard = false; editing = null; operationError = false; onNavigationLockChange(false) }) { Text(ui.s(Res.string.discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(ui.s(Res.string.keep_editing)) } },
    )
    if (editing != null) {
        VaccineRecordForm(editing!!, busy, operationError, onCancel = { discard = true }) { vaccine ->
            busy = true
            scope.launch {
                try {
                    operationError = false
                    val result = if (vaccine.id.isEmpty()) viewModel.add(vaccine) else viewModel.update(vaccine)
                    if (result.isSuccess) {
                        editing = null
                        reload()
                    } else operationError = true
                } finally { busy = false; if (editing == null) onNavigationLockChange(false) }
            }
        }
        return
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        EditorialHeader(ui.s(Res.string.vaccines), ui.s(Res.string.your_vaccination_records_are_saved_on_this_device), compact = true)
        when {
            loadError -> {
                Text(ui.s(Res.string.we_couldn_t_read_your_vaccinations_your_data), color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text(ui.s(Res.string.retry)) }
            }
            vaccines == null -> CircularProgressIndicator()
            else -> {
                Button(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = { operationError = false; editing = VaccineRecord("", ""); onNavigationLockChange(true) }) { Text(ui.s(Res.string.new_vaccination)) }
                if (vaccines!!.isEmpty()) Text(ui.s(Res.string.you_haven_t_recorded_any_vaccinations_yet))
                vaccines!!.forEach { vaccine ->
                    EditorialRecordCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EditorialStatus(vaccine.status.label(ui))
                            Text(vaccine.name, style = MaterialTheme.typography.titleMedium)
                            vaccine.dose?.let { VaccineRecordDetail(ui.s(Res.string.dose), it) }
                            val summaryDate = if (vaccine.status == VaccineStatus.ADMINISTERED) vaccine.administered else vaccine.scheduled
                            VaccineRecordDetail(if (vaccine.status == VaccineStatus.ADMINISTERED) ui.s(Res.string.administered) else ui.s(Res.string.scheduled), summaryDate?.let { dateLabel(ui, it) } ?: ui.s(Res.string.no_date_recorded))
                            if (vaccine.status != VaccineStatus.ADMINISTERED) vaccine.administered?.let { VaccineRecordDetail(ui.s(Res.string.recorded_administration_date), dateLabel(ui, it)) }
                            if (vaccine.status == VaccineStatus.ADMINISTERED) vaccine.scheduled?.let { VaccineRecordDetail(ui.s(Res.string.scheduled_date), dateLabel(ui, it)) }
                            vaccine.provider?.let { VaccineRecordDetail(ui.s(Res.string.professional_provider), it) }
                            vaccine.location?.let { VaccineRecordDetail(ui.s(Res.string.location), it) }
                            vaccine.proof?.let { VaccineRecordDetail(ui.s(Res.string.proof_reference), it) }
                            vaccine.notes?.let { VaccineRecordDetail(ui.s(Res.string.notes), it) }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled = !busy, onClick = { operationError = false; editing = vaccine; onNavigationLockChange(true) }) { Text(ui.s(Res.string.edit)) }
                                TextButton(enabled = !busy, onClick = { deleting = vaccine; onNavigationLockChange(true) }) { Text(ui.s(Res.string.delete)) }
                            }
                        }
                    }
                }
                if (operationError) Text(ui.s(Res.string.we_couldn_t_delete_the_vaccination_try_again), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { vaccine ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text(ui.s(Res.string.delete_vaccination)) },
            text = { Text(ui.s(Res.string.this_vaccination_will_be_deleted_from_this_device)) },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        operationError = false
                        val result = viewModel.delete(vaccine.id)
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
private fun VaccineRecordDetail(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value)
}

@Composable
private fun VaccineRecordForm(initial: VaccineRecord, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onSave: (VaccineRecord) -> Unit) {
    val ui = LocalUiCopy.current
    var name by retain(initial) { mutableStateOf(initial.name) }
    var dose by retain(initial) { mutableStateOf(initial.dose.orEmpty()) }
    var scheduled by retain(initial) { mutableStateOf(initial.scheduled.orEmpty()) }
    var administered by retain(initial) { mutableStateOf(initial.administered.orEmpty()) }
    var status by retain(initial) { mutableStateOf(initial.status) }
    var provider by retain(initial) { mutableStateOf(initial.provider.orEmpty()) }
    var location by retain(initial) { mutableStateOf(initial.location.orEmpty()) }
    var proof by retain(initial) { mutableStateOf(initial.proof.orEmpty()) }
    var notes by retain(initial) { mutableStateOf(initial.notes.orEmpty()) }
    var invalid by retain(initial) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().screenEntrance().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorialHeader(if (initial.id.isEmpty()) ui.s(Res.string.new_vaccination) else ui.s(Res.string.edit_vaccination))
        VaccineRecordTextField(ui.s(Res.string.vaccine), name, 512, busy, invalid && name.isBlank()) { name = it }
        VaccineRecordTextField(ui.s(Res.string.dose_optional), dose, 512, busy) { dose = it }
        DateField(ui.s(Res.string.scheduled_date_optional), scheduled, clearable = true, enabled = !busy) { scheduled = it }
        Text(ui.s(Res.string.status), style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaccineStatus.entries.forEach { option ->
                FilterChip(status == option, { status = option }, enabled = !busy, label = { Text(option.label(ui)) })
            }
        }
        DateField(if (status == VaccineStatus.ADMINISTERED) ui.s(Res.string.administration_date) else ui.s(Res.string.recorded_administration_date_optional), administered, clearable = true, enabled = !busy) { administered = it }
        Text(ui.s(Res.string.changing_status_preserves_the_recorded_administration_date_you), style = MaterialTheme.typography.bodySmall)
        VaccineRecordTextField(ui.s(Res.string.professional_provider_optional), provider, 512, busy) { provider = it }
        VaccineRecordTextField(ui.s(Res.string.location_optional), location, 512, busy) { location = it }
        VaccineRecordTextField(ui.s(Res.string.proof_reference_optional), proof, 512, busy) { proof = it }
        Text(ui.s(Res.string.the_reference_is_text_files_are_not_attached), style = MaterialTheme.typography.bodySmall)
        VaccineRecordTextField(ui.s(Res.string.notes_optional), notes, 4096, busy) { notes = it }
        Text(ui.s(Res.string.the_scheduled_date_does_not_create_an_appointment), style = MaterialTheme.typography.bodySmall)
        if (invalid) Text(ui.s(Res.string.enter_the_vaccine_and_if_administered_its_administration), color = MaterialTheme.colorScheme.error)
        if (saveError) Text(ui.s(Res.string.we_couldn_t_save_the_vaccination_your_changes), color = MaterialTheme.colorScheme.error)
        Button(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
            val vaccine = vaccineRecordForSave(initial, name, dose, scheduled, administered, status, provider, location, proof, notes)
            invalid = vaccine == null
            if (vaccine != null) onSave(vaccine)
        }) { Text(if (busy) ui.s(Res.string.saving) else ui.s(Res.string.save_vaccination)) }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = onCancel) { Text(ui.s(Res.string.cancel)) }
    }
}

@Composable
private fun VaccineRecordTextField(label: String, value: String, limit: Int, busy: Boolean, requiredError: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, enabled = !busy, singleLine = limit == 512, isError = requiredError || value.length > limit, supportingText = { Text("${value.length}/$limit") }, modifier = Modifier.fillMaxWidth())
}
