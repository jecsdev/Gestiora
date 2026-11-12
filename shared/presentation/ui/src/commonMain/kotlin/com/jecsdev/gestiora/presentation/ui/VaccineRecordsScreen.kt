package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.*
import com.jecsdev.gestiora.presentation.viewmodels.VaccineRecordViewModel
import kotlinx.coroutines.launch

@Composable
internal fun VaccineRecordsScreen(viewModel: VaccineRecordViewModel, onNavigationLockChange: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var vaccines by remember { mutableStateOf<List<VaccineRecord>?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var operationError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<VaccineRecord?>(null) }
    var deleting by remember { mutableStateOf<VaccineRecord?>(null) }
    var discard by remember { mutableStateOf(false) }
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
        title = { Text("¿Descartar cambios?") },
        text = { Text("Los cambios que no hayas guardado se perderán.") },
        confirmButton = { TextButton(onClick = { discard = false; editing = null; operationError = false; onNavigationLockChange(false) }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Seguir editando") } },
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
    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Vacunas", style = MaterialTheme.typography.headlineSmall)
        Text("Tus registros de vacunación se guardan en este dispositivo.")
        when {
            loadError -> {
                Text("No pudimos leer tus vacunas. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text("Reintentar") }
            }
            vaccines == null -> CircularProgressIndicator()
            else -> {
                Button(enabled = !busy, onClick = { operationError = false; editing = VaccineRecord("", ""); onNavigationLockChange(true) }) { Text("Nueva vacuna") }
                if (vaccines!!.isEmpty()) Text("Aún no tienes vacunas registradas.")
                vaccines!!.forEach { vaccine ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(vaccine.status.label(), style = MaterialTheme.typography.labelLarge)
                            Text(vaccine.name, style = MaterialTheme.typography.titleMedium)
                            vaccine.dose?.let { VaccineRecordDetail("Dosis", it) }
                            val summaryDate = if (vaccine.status == VaccineStatus.ADMINISTERED) vaccine.administered else vaccine.scheduled
                            VaccineRecordDetail(if (vaccine.status == VaccineStatus.ADMINISTERED) "Aplicada" else "Programada", summaryDate ?: "Sin fecha registrada")
                            if (vaccine.status != VaccineStatus.ADMINISTERED) vaccine.administered?.let { VaccineRecordDetail("Fecha de aplicación registrada", it) }
                            if (vaccine.status == VaccineStatus.ADMINISTERED) vaccine.scheduled?.let { VaccineRecordDetail("Fecha programada", it) }
                            vaccine.provider?.let { VaccineRecordDetail("Profesional / proveedor", it) }
                            vaccine.location?.let { VaccineRecordDetail("Lugar", it) }
                            vaccine.proof?.let { VaccineRecordDetail("Comprobante (referencia)", it) }
                            vaccine.notes?.let { VaccineRecordDetail("Notas", it) }
                            Row {
                                TextButton(enabled = !busy, onClick = { operationError = false; editing = vaccine; onNavigationLockChange(true) }) { Text("Editar") }
                                TextButton(enabled = !busy, onClick = { deleting = vaccine; onNavigationLockChange(true) }) { Text("Eliminar") }
                            }
                        }
                    }
                }
                if (operationError) Text("No pudimos eliminar la vacuna. Inténtalo de nuevo.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { vaccine ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text("¿Eliminar vacuna?") },
            text = { Text("Se eliminará esta vacuna de este dispositivo.") },
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
            }) { Text("Eliminar") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null; onNavigationLockChange(false) }) { Text("Cancelar") } },
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
    var name by remember(initial) { mutableStateOf(initial.name) }
    var dose by remember(initial) { mutableStateOf(initial.dose.orEmpty()) }
    var scheduled by remember(initial) { mutableStateOf(initial.scheduled.orEmpty()) }
    var administered by remember(initial) { mutableStateOf(initial.administered.orEmpty()) }
    var status by remember(initial) { mutableStateOf(initial.status) }
    var provider by remember(initial) { mutableStateOf(initial.provider.orEmpty()) }
    var location by remember(initial) { mutableStateOf(initial.location.orEmpty()) }
    var proof by remember(initial) { mutableStateOf(initial.proof.orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial.notes.orEmpty()) }
    var invalid by remember(initial) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (initial.id.isEmpty()) "Nueva vacuna" else "Editar vacuna", style = MaterialTheme.typography.headlineLarge)
        VaccineRecordTextField("Vacuna", name, 512, busy, invalid && name.isBlank()) { name = it }
        VaccineRecordTextField("Dosis (opcional)", dose, 512, busy) { dose = it }
        DateField("Fecha programada (opcional)", scheduled, clearable = true, enabled = !busy) { scheduled = it }
        Text("Estado", style = MaterialTheme.typography.labelLarge)
        VaccineStatus.entries.forEach { option ->
            FilterChip(status == option, { status = option }, enabled = !busy, label = { Text(option.label()) })
        }
        DateField(if (status == VaccineStatus.ADMINISTERED) "Fecha de aplicación" else "Fecha de aplicación registrada (opcional)", administered, clearable = true, enabled = !busy) { administered = it }
        Text("Cambiar el estado conserva la fecha de aplicación registrada. Puedes borrarla explícitamente si necesitas corregirla.", style = MaterialTheme.typography.bodySmall)
        VaccineRecordTextField("Profesional / proveedor (opcional)", provider, 512, busy) { provider = it }
        VaccineRecordTextField("Lugar (opcional)", location, 512, busy) { location = it }
        VaccineRecordTextField("Comprobante (referencia, opcional)", proof, 512, busy) { proof = it }
        Text("La referencia es texto; no se adjuntan archivos en esta sección.", style = MaterialTheme.typography.bodySmall)
        VaccineRecordTextField("Notas (opcional)", notes, 4096, busy) { notes = it }
        Text("La fecha programada no crea una cita ni una notificación.", style = MaterialTheme.typography.bodySmall)
        if (invalid) Text("Indica la vacuna y, si está aplicada, la fecha de aplicación. Revisa las fechas y los límites de los campos.", color = MaterialTheme.colorScheme.error)
        if (saveError) Text("No pudimos guardar la vacuna. Tus cambios siguen en el formulario.", color = MaterialTheme.colorScheme.error)
        Button(enabled = !busy, onClick = {
            val vaccine = vaccineRecordForSave(initial, name, dose, scheduled, administered, status, provider, location, proof, notes)
            invalid = vaccine == null
            if (vaccine != null) onSave(vaccine)
        }) { Text(if (busy) "Guardando…" else "Guardar vacuna") }
        OutlinedButton(enabled = !busy, onClick = onCancel) { Text("Cancelar") }
    }
}

@Composable
private fun VaccineRecordTextField(label: String, value: String, limit: Int, busy: Boolean, requiredError: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, enabled = !busy, singleLine = limit == 512, isError = requiredError || value.length > limit, supportingText = { Text("${value.length}/$limit") }, modifier = Modifier.fillMaxWidth())
}
