package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.*
import com.jecsdev.gestiora.presentation.viewmodels.VitalRecordViewModel
import kotlinx.coroutines.launch

@Composable
internal fun VitalRecordsScreen(viewModel: VitalRecordViewModel, onNavigationLockChange: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<VitalRecord>?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var operationError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<VitalRecord?>(null) }
    var deleting by remember { mutableStateOf<VitalRecord?>(null) }
    var discard by remember { mutableStateOf(false) }
    suspend fun reload() {
        val result = viewModel.load()
        records = result.getOrNull()
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
        title = { Text("¿Descartar cambios?") },
        text = { Text("Los cambios que no hayas guardado se perderán.") },
        confirmButton = { TextButton(onClick = { discard = false; editing = null; operationError = false; onNavigationLockChange(false) }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Seguir editando") } },
    )
    if (editing != null) {
        VitalRecordForm(editing!!, busy, operationError, onCancel = { discard = true }) { record ->
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
        return
    }
    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Historial de salud", style = MaterialTheme.typography.headlineSmall)
        Text("Guarda la información indicada por tu equipo de salud. Tus registros se conservan en este dispositivo.")
        when {
            loadError -> {
                Text("No pudimos leer tus registros de salud. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text("Reintentar") }
            }
            records == null -> CircularProgressIndicator()
            else -> {
                Button(enabled = !busy, onClick = { operationError = false; editing = VitalRecord("", todayIsoDate(), VitalType.WEIGHT); onNavigationLockChange(true) }) { Text("Nuevo registro de salud") }
                LatestVitalSummaries(records!!)
                if (records!!.isEmpty()) Text("Aún no tienes registros de salud.")
                orderedVitalRecords(records!!).forEach { record ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (record.type == VitalType.CUSTOM) record.label.orEmpty() else record.type.label(), style = MaterialTheme.typography.titleMedium)
                            Text(record.date, style = MaterialTheme.typography.labelLarge)
                            Text(vitalRecordValue(record), style = MaterialTheme.typography.titleLarge)
                            record.notes?.let { Text(it) }
                            Row {
                                TextButton(enabled = !busy, onClick = { operationError = false; editing = record; onNavigationLockChange(true) }) { Text("Editar") }
                                TextButton(enabled = !busy, onClick = { deleting = record; onNavigationLockChange(true) }) { Text("Eliminar") }
                            }
                        }
                    }
                }
                if (operationError) Text("No pudimos eliminar el registro. Inténtalo de nuevo.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { record ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text("¿Eliminar registro?") },
            text = { Text("Se eliminará este registro de este dispositivo.") },
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
            }) { Text("Eliminar") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null; onNavigationLockChange(false) }) { Text("Cancelar") } },
        )
    }
}



@Composable
private fun LatestVitalSummaries(records: List<VitalRecord>) {
    val weight = latestVitalRecord(records, VitalType.WEIGHT)
    val pressure = latestVitalRecord(records, VitalType.BLOOD_PRESSURE)
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Último peso", style = MaterialTheme.typography.titleMedium)
            Text(weight?.let(::vitalRecordValue) ?: "Sin registros", style = MaterialTheme.typography.headlineSmall)
            weight?.let { Text(it.date, style = MaterialTheme.typography.labelMedium) }
        }
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Presión arterial", style = MaterialTheme.typography.titleMedium)
            Text(pressure?.let(::vitalRecordValue) ?: "Sin registros", style = MaterialTheme.typography.headlineSmall)
            pressure?.let { Text(it.date, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun VitalRecordForm(initial: VitalRecord, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onSave: (VitalRecord) -> Unit) {
    var date by remember(initial) { mutableStateOf(initial.date) }
    var type by remember(initial) { mutableStateOf(initial.type) }
    var value by remember(initial) { mutableStateOf(initial.value?.let(::formatVitalNumber).orEmpty()) }
    var value2 by remember(initial) { mutableStateOf(initial.value2?.let(::formatVitalNumber).orEmpty()) }
    var label by remember(initial) { mutableStateOf(initial.label.orEmpty()) }
    var text by remember(initial) { mutableStateOf(initial.text.orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial.notes.orEmpty()) }
    var invalid by remember(initial) { mutableStateOf(false) }
    var typeMenu by remember(initial) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (initial.id.isEmpty()) "Nuevo registro de salud" else "Editar registro de salud", style = MaterialTheme.typography.headlineLarge)
        DateField("Fecha", date, enabled = !busy) { date = it }
        Text("Tipo de registro", style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(enabled = !busy, onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(type.label()) }
            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                VitalType.entries.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label()) }, onClick = { type = option; invalid = false; typeMenu = false })
                }
            }
        }
        if (type == VitalType.CUSTOM) {
            OutlinedTextField(label, { label = it }, label = { Text("Nombre de la medida") }, singleLine = true, enabled = !busy, isError = label.length > 512 || (invalid && label.isBlank()), supportingText = { Text("${label.length}/512") }, modifier = Modifier.fillMaxWidth())
        }
        if (type.isNumeric) {
            val parsed = parseVitalNumber(value)
            OutlinedTextField(value, { value = it }, label = { Text(if (type == VitalType.BLOOD_PRESSURE) "Sistólica (mmHg)" else "Valor") }, singleLine = true, enabled = !busy,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                isError = invalid && (parsed == null || parsed !in 0.0..400.0), supportingText = { Text("Número entre 0 y 400. Puedes usar punto o coma decimal.") }, modifier = Modifier.fillMaxWidth())
            if (type == VitalType.BLOOD_PRESSURE) {
                val parsed2 = parseVitalNumber(value2)
                OutlinedTextField(value2, { value2 = it }, label = { Text("Diastólica (mmHg)") }, singleLine = true, enabled = !busy,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    isError = invalid && (parsed2 == null || parsed2 !in 20.0..200.0), supportingText = { Text("Número entre 20 y 200.") }, modifier = Modifier.fillMaxWidth())
            }
        } else {
            OutlinedTextField(text, { text = it }, label = { Text("Descripción") }, enabled = !busy, isError = text.length > 4096 || (invalid && text.isBlank()), supportingText = { Text("${text.length}/4096") }, modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notas (opcional)") }, enabled = !busy, isError = notes.length > 4096, supportingText = { Text("${notes.length}/4096") }, modifier = Modifier.fillMaxWidth())
        if (invalid) Text("Revisa la fecha y los campos requeridos. Respeta los límites indicados.", color = MaterialTheme.colorScheme.error)
        if (saveError) Text("No pudimos guardar el registro. Tus cambios siguen en el formulario.", color = MaterialTheme.colorScheme.error)
        Button(enabled = !busy, onClick = {
            val record = vitalRecordForSave(initial, date, type, value, value2, label, text, notes)
            invalid = record == null
            if (record != null) onSave(record)
        }) { Text(if (busy) "Guardando…" else "Guardar registro") }
        OutlinedButton(enabled = !busy, onClick = onCancel) { Text("Cancelar") }
    }
}
