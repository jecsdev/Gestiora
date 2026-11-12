package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.*
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalNoteViewModel
import kotlinx.coroutines.launch

private fun NoteCategory.label() = when (this) {
    NoteCategory.MEDICAL_INSTRUCTIONS -> "Indicaciones médicas"
    NoteCategory.DOCTOR_QUESTIONS -> "Preguntas para el doctor"
    NoteCategory.MEDICATIONS -> "Medicamentos"
    NoteCategory.NUTRITION -> "Alimentación"
    NoteCategory.SYMPTOMS -> "Síntomas"
    NoteCategory.GENERAL -> "Notas generales"
}
private fun NoteTaskStatus.label() = when (this) {
    NoteTaskStatus.NONE -> "Sin tarea"
    NoteTaskStatus.PENDING -> "Pendiente"
    NoteTaskStatus.COMPLETED -> "Completada"
}

@Composable
internal fun PrenatalNotesScreen(viewModel: PrenatalNoteViewModel, initialStatus: NoteTaskStatus? = null, onNavigationLockChange: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf<List<PrenatalNote>?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var operationError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PrenatalNote?>(null) }
    var deleting by remember { mutableStateOf<PrenatalNote?>(null) }
    var discard by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(initialStatus) }
    suspend fun reload() {
        val result = viewModel.load()
        notes = result.getOrNull()
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
        PrenatalNoteForm(editing!!, busy, operationError, onCancel = { discard = true }) { note ->
            busy = true
            scope.launch {
                try {
                    operationError = false
                    val result = if (note.id.isEmpty()) viewModel.add(note) else viewModel.update(note)
                    if (result.isSuccess) {
                        query = ""
                        if (statusFilter != null) statusFilter = note.status
                        editing = null
                        reload()
                    } else operationError = true
                } finally { busy = false; if (editing == null) onNavigationLockChange(false) }
            }
        }
        return
    }
    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Notas y seguimiento", style = MaterialTheme.typography.headlineSmall)
        Text("Tus notas se guardan en este dispositivo.")
        when {
            loadError -> {
                Text("No pudimos leer tus notas. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text("Reintentar") }
            }
            notes == null -> CircularProgressIndicator()
            else -> {
                OutlinedTextField(query, { query = it }, label = { Text("Buscar en título y contenido") }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(statusFilter == null, { statusFilter = null }, enabled = !busy, label = { Text("Todas") })
                    NoteTaskStatus.entries.forEach { option ->
                        FilterChip(statusFilter == option, { statusFilter = option }, enabled = !busy, label = { Text(option.label()) })
                    }
                }
                Button(enabled = !busy, onClick = { operationError = false; editing = PrenatalNote("", ""); onNavigationLockChange(true) }) { Text("Nueva nota") }
                val visibleNotes = filterNotes(notes!!, query, statusFilter)
                if (notes!!.isEmpty()) Text("Aún no tienes notas registradas.")
                else if (visibleNotes.isEmpty()) Text("No hay notas que coincidan con estos filtros.")
                if (query.isNotEmpty() || statusFilter != null) TextButton(enabled = !busy, onClick = { query = ""; statusFilter = null }) { Text("Limpiar filtros") }
                visibleNotes.forEach { note ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(note.title, style = MaterialTheme.typography.titleMedium)
                            Text(note.category.label(), style = MaterialTheme.typography.labelMedium)
                            note.body?.let { Text(it) }
                            Text(note.status.label())
                            note.followUp?.let { Text("Seguimiento: $it") }
                            Row {
                                TextButton(enabled = !busy, onClick = { operationError = false; editing = note; onNavigationLockChange(true) }) { Text("Editar") }
                                TextButton(enabled = !busy, onClick = { deleting = note; onNavigationLockChange(true) }) { Text("Eliminar") }
                            }
                        }
                    }
                }
                if (operationError) Text("No pudimos eliminar la nota. Inténtalo de nuevo.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { note ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text("¿Eliminar nota?") },
            text = { Text("Se eliminará esta nota de este dispositivo.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        operationError = false
                        val result = viewModel.delete(note.id)
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
private fun PrenatalNoteForm(initial: PrenatalNote, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onSave: (PrenatalNote) -> Unit) {
    var title by remember(initial) { mutableStateOf(initial.title) }
    var body by remember(initial) { mutableStateOf(initial.body.orEmpty()) }
    var category by remember(initial) { mutableStateOf(initial.category) }
    var status by remember(initial) { mutableStateOf(initial.status) }
    var followUp by remember(initial) { mutableStateOf(initial.followUp.orEmpty()) }
    var invalid by remember(initial) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (initial.id.isEmpty()) "Nueva nota" else "Editar nota", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, enabled = !busy, isError = title.length > 512, supportingText = { Text("${title.length}/512") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(body, { body = it }, label = { Text("Contenido (opcional)") }, enabled = !busy, isError = body.length > 4096, supportingText = { Text("${body.length}/4096") }, modifier = Modifier.fillMaxWidth())
        Text("Categoría", style = MaterialTheme.typography.labelLarge)
        NoteCategory.entries.forEach { option -> FilterChip(category == option, { category = option }, enabled = !busy, label = { Text(option.label()) }) }
        Text("Seguimiento", style = MaterialTheme.typography.labelLarge)
        NoteTaskStatus.entries.forEach { option -> FilterChip(status == option, { status = option }, enabled = !busy, label = { Text(option.label()) }) }
        DateField("Fecha de seguimiento (opcional)", followUp, clearable = true, enabled = !busy) { followUp = it }
        Text("La fecha de seguimiento no genera una notificación.", style = MaterialTheme.typography.bodySmall)
        if (invalid) Text("Indica un título y respeta los límites de los campos.", color = MaterialTheme.colorScheme.error)
        if (saveError) Text("No pudimos guardar la nota. Tus cambios siguen en el formulario.", color = MaterialTheme.colorScheme.error)
        Button(enabled = !busy, onClick = {
            invalid = title.isBlank() || title.length > 512 || body.length > 4096 || (followUp.isNotEmpty() && isoToUtcMillis(followUp) == null)
            if (!invalid) onSave(initial.copy(title = title.trim(), body = body.trim().ifEmpty { null }, category = category, status = status, followUp = followUp.ifEmpty { null }))
        }) { Text(if (busy) "Guardando…" else "Guardar nota") }
        OutlinedButton(enabled = !busy, onClick = onCancel) { Text("Cancelar") }
    }
}
