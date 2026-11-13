package com.jecsdev.gestiora.presentation.ui

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
internal fun ExamRecordsScreen(viewModel: ExamRecordViewModel, onNavigationLockChange: (Boolean) -> Unit) {
    val scope = retainedOperationScope()
    var exams by retain { mutableStateOf<List<ExamRecord>?>(null) }
    var loadError by retain { mutableStateOf(false) }
    var operationError by retain { mutableStateOf(false) }
    var busy by retain { mutableStateOf(false) }
    var editing by retain { mutableStateOf<ExamRecord?>(null) }
    var deleting by retain { mutableStateOf<ExamRecord?>(null) }
    var discard by retain { mutableStateOf(false) }
    suspend fun reload() {
        val result = viewModel.load()
        exams = result.getOrNull()
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
        ExamRecordForm(editing!!, busy, operationError, onCancel = { discard = true }) { exam ->
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
        return
    }
    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Estudios", style = MaterialTheme.typography.headlineSmall)
        Text("Tus estudios y resultados se guardan en este dispositivo.")
        when {
            loadError -> {
                Text("No pudimos leer tus estudios. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text("Reintentar") }
            }
            exams == null -> CircularProgressIndicator()
            else -> {
                Button(enabled = !busy, onClick = { operationError = false; editing = ExamRecord("", "", requested = todayIsoDate()); onNavigationLockChange(true) }) { Text("Nuevo estudio") }
                if (exams!!.isEmpty()) Text("Aún no tienes estudios registrados.")
                exams!!.forEach { exam ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(exam.status.label(), style = MaterialTheme.typography.labelLarge)
                            Text(exam.name, style = MaterialTheme.typography.titleMedium)
                            Text(exam.kind.label(), style = MaterialTheme.typography.labelMedium)
                            ExamRecordDetail("Fecha de referencia", exam.completed ?: exam.scheduled ?: exam.requested ?: "Sin fecha registrada")
                            exam.requested?.let { ExamRecordDetail("Fecha de solicitud", it) }
                            exam.scheduled?.let { ExamRecordDetail("Fecha programada", it) }
                            exam.completed?.let { ExamRecordDetail("Fecha de realización registrada", it) }
                            exam.physician?.let { ExamRecordDetail("Médico que lo solicitó", it) }
                            exam.results?.let { ExamRecordDetail("Resumen de resultados", it) }
                            exam.notes?.let { ExamRecordDetail("Observaciones médicas", it) }
                            exam.attachment?.let { ExamRecordDetail("Documento o imagen (referencia)", it) }
                            Row {
                                TextButton(enabled = !busy, onClick = { operationError = false; editing = exam; onNavigationLockChange(true) }) { Text("Editar") }
                                TextButton(enabled = !busy, onClick = { deleting = exam; onNavigationLockChange(true) }) { Text("Eliminar") }
                            }
                        }
                    }
                }
                if (operationError) Text("No pudimos eliminar el estudio. Inténtalo de nuevo.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { exam ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text("¿Eliminar estudio?") },
            text = { Text("Se eliminará este estudio de este dispositivo.") },
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
            }) { Text("Eliminar") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null; onNavigationLockChange(false) }) { Text("Cancelar") } },
        )
    }
}




@Composable
private fun ExamRecordDetail(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value)
}

@Composable
private fun ExamRecordForm(initial: ExamRecord, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onSave: (ExamRecord) -> Unit) {
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
    var invalid by retain(initial) { mutableStateOf(false) }
    var kindMenu by retain(initial) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (initial.id.isEmpty()) "Nuevo estudio" else "Editar estudio", style = MaterialTheme.typography.headlineLarge)
        ExamRecordTextField("Nombre del estudio", name, 512, busy, invalid && name.isBlank()) { name = it }
        Text("Tipo", style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(enabled = !busy, onClick = { kindMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(kind.label()) }
            DropdownMenu(expanded = kindMenu, onDismissRequest = { kindMenu = false }) {
                ExamKind.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label()) }, onClick = { kind = option; kindMenu = false }) }
            }
        }
        DateField("Fecha de solicitud (opcional)", requested, clearable = true, enabled = !busy) { requested = it }
        DateField("Fecha programada (opcional)", scheduled, clearable = true, enabled = !busy) { scheduled = it }
        Text("Estado", style = MaterialTheme.typography.labelLarge)
        ExamStatus.entries.forEach { option -> FilterChip(status == option, { status = option }, enabled = !busy, label = { Text(option.label()) }) }
        DateField(if (status == ExamStatus.COMPLETED) "Fecha de realización" else "Fecha de realización registrada (opcional)", completed, clearable = true, enabled = !busy) { completed = it }
        Text("Cambiar el estado conserva la fecha de realización registrada. Puedes borrarla explícitamente si necesitas corregirla.", style = MaterialTheme.typography.bodySmall)
        ExamRecordTextField("Médico que lo solicitó (opcional)", physician, 512, busy) { physician = it }
        ExamRecordTextField("Resumen de resultados (opcional)", results, 4096, busy) { results = it }
        ExamRecordTextField("Observaciones médicas (opcional)", notes, 4096, busy) { notes = it }
        ExamRecordTextField("Documento o imagen (referencia, opcional)", attachment, 512, busy) { attachment = it }
        Text("La referencia es texto; no se adjuntan archivos en esta sección.", style = MaterialTheme.typography.bodySmall)
        Text("Las fechas no crean citas ni notificaciones.", style = MaterialTheme.typography.bodySmall)
        if (invalid) Text("Indica el estudio y, si está completado, la fecha de realización. Revisa las fechas y los límites de los campos.", color = MaterialTheme.colorScheme.error)
        if (saveError) Text("No pudimos guardar el estudio. Tus cambios siguen en el formulario.", color = MaterialTheme.colorScheme.error)
        Button(enabled = !busy, onClick = {
            val exam = examRecordForSave(initial, name, kind, requested, scheduled, completed, status, physician, results, notes, attachment)
            invalid = exam == null
            if (exam != null) onSave(exam)
        }) { Text(if (busy) "Guardando…" else "Guardar estudio") }
        OutlinedButton(enabled = !busy, onClick = onCancel) { Text("Cancelar") }
    }
}

@Composable
private fun ExamRecordTextField(label: String, value: String, limit: Int, busy: Boolean, requiredError: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, enabled = !busy, singleLine = limit == 512, isError = requiredError || value.length > limit, supportingText = { Text("${value.length}/$limit") }, modifier = Modifier.fillMaxWidth())
}
