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
import com.jecsdev.gestiora.presentation.viewmodels.VitalRecordViewModel
import kotlinx.coroutines.launch

@Composable
internal fun VitalRecordsScreen(viewModel: VitalRecordViewModel, onNavigationLockChange: (Boolean) -> Unit) {
    val ui = LocalUiCopy.current
    val scope = retainedOperationScope()
    var weightUnit by retain { mutableStateOf(WeightUnit.KILOGRAMS) }
    var records by retain { mutableStateOf<List<VitalRecord>?>(null) }
    var loadError by retain { mutableStateOf(false) }
    var operationError by retain { mutableStateOf(false) }
    var busy by retain { mutableStateOf(false) }
    var editing by retain { mutableStateOf<VitalRecord?>(null) }
    var deleting by retain { mutableStateOf<VitalRecord?>(null) }
    suspend fun reload() {
        val result = viewModel.load()
        records = result.getOrNull()
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
            VitalRecordForm(editing!!, busy, operationError, weightUnit, { weightUnit = it }, onCancel = onCancel, onTextChanges = onTextChanges) { record ->
                busy = true
                scope.launch {
                    try {
                        operationError = false
                        val result = if (record.id.isEmpty()) viewModel.add(record) else viewModel.update(record)
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
        EditorialHeader(ui.s(Res.string.health_history), ui.s(Res.string.save_information_provided_by_your_care_team_your), compact = true)
        when {
            loadError -> {
                Text(ui.s(Res.string.we_couldn_t_read_your_health_records_your), color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text(ui.s(Res.string.retry)) }
            }
            records == null -> CircularProgressIndicator()
            else -> {
                GradientButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = { operationError = false; editing = VitalRecord("", todayIsoDate(), VitalType.WEIGHT); onNavigationLockChange(true) }) { Text(ui.s(Res.string.new_health_record)) }
                WeightUnitSelector(weightUnit, enabled = !busy) { weightUnit = it }
                LatestVitalSummaries(records!!, weightUnit)
                if (records!!.isEmpty()) EditorialEmptyState(ui.s(Res.string.you_haven_t_recorded_any_health_data_yet))
                orderedVitalRecords(records!!).forEach { record ->
                    EditorialRecordCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (record.type == VitalType.CUSTOM) record.label.orEmpty() else record.type.label(ui), style = MaterialTheme.typography.titleMedium)
                            Text(dateLabel(ui, record.date), style = MaterialTheme.typography.labelLarge)
                            Text(vitalRecordValue(record, ui, weightUnit), style = MaterialTheme.typography.titleLarge)
                            record.notes?.let { Text(it) }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled = !busy, onClick = { operationError = false; editing = record; onNavigationLockChange(true) }) { Text(ui.s(Res.string.edit)) }
                                TextButton(enabled = !busy, onClick = { deleting = record; onNavigationLockChange(true) }) { Text(ui.s(Res.string.delete)) }
                            }
                        }
                    }
                }
                if (operationError) Text(ui.s(Res.string.we_couldn_t_delete_the_record_try_again), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { record ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text(ui.s(Res.string.delete_record)) },
            text = { Text(ui.s(Res.string.this_record_will_be_deleted_from_this_device)) },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        operationError = false
                        val result = viewModel.delete(record.id)
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
private fun LatestVitalSummaries(records: List<VitalRecord>, weightUnit: WeightUnit) {
    val ui = LocalUiCopy.current
    val weight = latestVitalRecord(records, VitalType.WEIGHT)
    val pressure = latestVitalRecord(records, VitalType.BLOOD_PRESSURE)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f
        val weightCard: @Composable (Modifier) -> Unit = { modifier ->
            Card(modifier, shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ui.s(Res.string.latest_weight), style = MaterialTheme.typography.labelMedium)
                    Text(weight?.let { vitalRecordValue(it, ui, weightUnit) } ?: ui.s(Res.string.no_records), style = MaterialTheme.typography.headlineSmall)
                    weight?.let { Text(dateLabel(ui, it.date), style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
        val pressureCard: @Composable (Modifier) -> Unit = { modifier ->
            Card(modifier, shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ui.s(Res.string.blood_pressure), style = MaterialTheme.typography.labelMedium)
                    Text(pressure?.let { vitalRecordValue(it, ui) } ?: ui.s(Res.string.no_records), style = MaterialTheme.typography.headlineSmall)
                    pressure?.let { Text(dateLabel(ui, it.date), style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
        if (maxWidth < 300.dp || largeText) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            weightCard(Modifier.fillMaxWidth()); pressureCard(Modifier.fillMaxWidth())
        } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            weightCard(Modifier.weight(1f)); pressureCard(Modifier.weight(1f))
        }
    }
}

@Composable
private fun VitalRecordForm(initial: VitalRecord, busy: Boolean, saveError: Boolean, preferredWeightUnit: WeightUnit, onWeightUnitChange: (WeightUnit) -> Unit, onCancel: () -> Unit, onTextChanges: (Boolean) -> Unit, onSave: (VitalRecord) -> Unit) {
    val ui = LocalUiCopy.current
    var date by retain(initial) { mutableStateOf(initial.date) }
    var type by retain(initial) { mutableStateOf(initial.type) }
    var value by retain(initial) { mutableStateOf(initial.value?.let(::formatVitalNumber).orEmpty()) }
    var weightEntry by retain(initial) { mutableStateOf(WeightEntry.fromKilograms(initial.value.takeIf { initial.type == VitalType.WEIGHT }, preferredWeightUnit)) }
    var value2 by retain(initial) { mutableStateOf(initial.value2?.let(::formatVitalNumber).orEmpty()) }
    var label by retain(initial) { mutableStateOf(initial.label.orEmpty()) }
    var text by retain(initial) { mutableStateOf(initial.text.orEmpty()) }
    var notes by retain(initial) { mutableStateOf(initial.notes.orEmpty()) }
    var invalid by retain(initial) { mutableStateOf(false) }
    var typeMenu by retain(initial) { mutableStateOf(false) }
    val initialWeightInput = WeightEntry.fromKilograms(initial.value.takeIf { initial.type == VitalType.WEIGHT }, weightEntry.unit).input
    SideEffect {
        onTextChanges(
            value.hasTextChangedFrom(initial.value?.let(::formatVitalNumber)) ||
                value2.hasTextChangedFrom(initial.value2?.let(::formatVitalNumber)) ||
                label.hasTextChangedFrom(initial.label) ||
                text.hasTextChangedFrom(initial.text) ||
                notes.hasTextChangedFrom(initial.notes) ||
                weightEntry.input.hasTextChangedFrom(initialWeightInput),
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorialHeader(if (initial.id.isEmpty()) ui.s(Res.string.new_health_record) else ui.s(Res.string.edit_health_record))
        DateField(ui.s(Res.string.date), date, enabled = !busy) { date = it }
        Text(ui.s(Res.string.record_type), style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(enabled = !busy, onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(type.label(ui)) }
            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                VitalType.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label(ui)) }, onClick = { type = option; invalid = false; typeMenu = false })
                }
            }
        }
        if (type == VitalType.CUSTOM) {
            PastelTextField(label, { label = it }, label = { Text(ui.s(Res.string.measurement_name)) }, singleLine = true, enabled = !busy, isError = label.length > 512 || (invalid && label.isBlank()), supportingText = { Text("${label.length}/512") }, modifier = Modifier.fillMaxWidth())
        }
        if (type.isNumeric) {
            val isWeight = type == VitalType.WEIGHT
            if (isWeight) WeightUnitSelector(weightEntry.unit, enabled = !busy && (weightEntry.input.isBlank() || weightEntry.kilograms != null)) {
                weightEntry = weightEntry.switchTo(it)
                onWeightUnitChange(it)
            }
            val parsed = if (isWeight) weightEntry.kilograms else parseVitalNumber(value)
            PastelTextField(if (isWeight) weightEntry.input else value, { if (isWeight) weightEntry = weightEntry.edit(it) else value = it },
                label = { Text(when {
                    isWeight -> ui.s(if (weightEntry.unit == WeightUnit.KILOGRAMS) Res.string.weight_input_kg else Res.string.weight_input_lb)
                    type == VitalType.BLOOD_PRESSURE -> ui.s(Res.string.systolic_mmhg)
                    else -> ui.s(Res.string.value)
                }) }, singleLine = true, enabled = !busy,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                isError = invalid && (parsed == null || parsed !in 0.0..400.0), supportingText = { Text(if (isWeight) ui.s(Res.string.weight_input_range) else ui.s(Res.string.number_between_0_and_400_you_can_use)) }, modifier = Modifier.fillMaxWidth())
            if (isWeight && weightEntry.kilograms != null) Text(weightDisplay(weightEntry.kilograms, weightEntry.unit, ui), style = MaterialTheme.typography.bodySmall)
            if (type == VitalType.BLOOD_PRESSURE) {
                val parsed2 = parseVitalNumber(value2)
                PastelTextField(value2, { value2 = it }, label = { Text(ui.s(Res.string.diastolic_mmhg)) }, singleLine = true, enabled = !busy,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    isError = invalid && (parsed2 == null || parsed2 !in 20.0..200.0), supportingText = { Text(ui.s(Res.string.number_between_20_and_200)) }, modifier = Modifier.fillMaxWidth())
            }
        } else {
            PastelTextField(text, { text = it }, label = { Text(ui.s(Res.string.description)) }, enabled = !busy, isError = text.length > 4096 || (invalid && text.isBlank()), supportingText = { Text("${text.length}/4096") }, modifier = Modifier.fillMaxWidth())
        }
        PastelTextField(notes, { notes = it }, label = { Text(ui.s(Res.string.notes_optional)) }, enabled = !busy, isError = notes.length > 4096, supportingText = { Text("${notes.length}/4096") }, modifier = Modifier.fillMaxWidth())
        if (invalid) Text(ui.s(Res.string.check_the_date_and_required_fields_respect_the), color = MaterialTheme.colorScheme.error)
        if (saveError) Text(ui.s(Res.string.we_couldn_t_save_the_record_your_changes), color = MaterialTheme.colorScheme.error)
        GradientButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
            val record = vitalRecordForSave(initial, date, type, if (type == VitalType.WEIGHT) weightEntry.kilograms?.let(::formatVitalNumber).orEmpty() else value, value2, label, text, notes)
            invalid = record == null
            if (record != null) onSave(record)
        }) { Text(if (busy) ui.s(Res.string.saving) else ui.s(Res.string.save_record)) }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = onCancel) { Text(ui.s(Res.string.cancel)) }
    }
}
