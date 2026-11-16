package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.PersonalReminder
import com.jecsdev.gestiora.domain.models.ReminderKind
import com.jecsdev.gestiora.presentation.viewmodels.PersonalReminderViewModel
import kotlinx.coroutines.launch

private fun ReminderKind.label() = when (this) {
    ReminderKind.APPOINTMENT -> "Cita"
    ReminderKind.EXAM -> "Estudio"
    ReminderKind.VACCINE -> "Vacuna"
    ReminderKind.FOLLOW_UP -> "Seguimiento"
    ReminderKind.PERSONAL -> "Personal"
}

@Composable
internal fun PersonalRemindersScreen(viewModel: PersonalReminderViewModel, onNavigationLockChange: (Boolean) -> Unit) {
    val scope = retainedOperationScope()
    var records by retain { mutableStateOf<List<PersonalReminder>?>(null) }
    var loadError by retain { mutableStateOf(false) }
    var saveError by retain { mutableStateOf(false) }
    var busy by retain { mutableStateOf(false) }
    var editing by retain { mutableStateOf<PersonalReminder?>(null) }
    var deleting by retain { mutableStateOf<PersonalReminder?>(null) }
    var discard by retain { mutableStateOf(false) }
    var selectedDate by retain { mutableStateOf("") }
    var month by retain { mutableStateOf(todayIsoDate().take(7)) }
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
        onDismissRequest = { discard = false }, title = { Text("¿Descartar cambios?") },
        text = { Text("Los cambios que no hayas guardado se perderán.") },
        confirmButton = { TextButton(onClick = { discard = false; editing = null; saveError = false; onNavigationLockChange(false) }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Seguir editando") } },
    )
    editing?.let { initial ->
        PersonalReminderForm(initial, busy, saveError, onCancel = { discard = true }) { record ->
            busy = true
            scope.launch {
                try {
                    saveError = false
                    val result = if (record.id.isEmpty()) viewModel.add(record) else viewModel.update(record)
                    if (result.isSuccess) {
                        if (selectedDate.isNotEmpty()) { selectedDate = record.date; month = record.date.take(7) }
                        editing = null
                        reload()
                    } else saveError = true
                } finally { busy = false; if (editing == null) onNavigationLockChange(false) }
            }
        }
        return
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        EditorialHeader("Recordatorios", "Organiza tus pendientes en este dispositivo. Los recordatorios aparecen dentro de Gestiora; no envían notificaciones.")
        when {
            loadError -> {
                Text("No pudimos leer tus recordatorios. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true; onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text("Reintentar") }
            }
            records == null -> CircularProgressIndicator()
            else -> {
                AppointmentCalendar(month, selectedDate, todayIsoDate(), records!!.map { it.date }.toSet(), !busy,
                    onMonthChange = { month = it }, onDateSelect = { selectedDate = it; month = it.take(7) }, markedDescription = "tiene recordatorios")
                Button(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
                    saveError = false; editing = PersonalReminder("", "", selectedDate.ifEmpty { todayIsoDate() }); onNavigationLockChange(true)
                }) { Text("Nuevo recordatorio") }
                if (selectedDate.isNotEmpty()) {
                    Text("Recordatorios del ${dateLabel(selectedDate)}", style = MaterialTheme.typography.titleMedium)
                    TextButton(enabled = !busy, onClick = { selectedDate = "" }) { Text("Ver todos los recordatorios") }
                }
                val visible = records!!.filter { selectedDate.isEmpty() || it.date == selectedDate }
                    .sortedWith(compareBy<PersonalReminder> { it.date }.thenBy { it.time.orEmpty() }.thenBy { it.id })
                if (visible.isEmpty()) Text("No tienes recordatorios para esta selección.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                visible.forEach { record ->
                    EditorialRecordCard {
                        EditorialStatus(if (record.done) "Completado" else record.kind.label())
                        Text(record.title, style = MaterialTheme.typography.titleMedium)
                        Text("${dateLabel(record.date)} · ${record.time ?: "Todo el día"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(enabled = !busy, onClick = { saveError = false; editing = record; onNavigationLockChange(true) }) { Text("Editar") }
                            TextButton(enabled = !busy, onClick = { deleting = record; onNavigationLockChange(true) }) { Text("Eliminar") }
                        }
                    }
                }
                if (saveError) Text("No pudimos eliminar el recordatorio. Inténtalo de nuevo.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { record ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text("¿Eliminar recordatorio?") }, text = { Text("Se eliminará este recordatorio de este dispositivo.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        saveError = false
                        val result = viewModel.delete(record.id)
                        deleting = null
                        if (result.isSuccess) reload() else saveError = true
                    } finally { busy = false; onNavigationLockChange(false) }
                }
            }) { Text("Eliminar") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null; onNavigationLockChange(false) }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PersonalReminderForm(initial: PersonalReminder, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onSave: (PersonalReminder) -> Unit) {
    var title by retain(initial) { mutableStateOf(initial.title) }
    var date by retain(initial) { mutableStateOf(initial.date) }
    var time by retain(initial) { mutableStateOf(initial.time.orEmpty()) }
    var kind by retain(initial) { mutableStateOf(initial.kind) }
    var done by retain(initial) { mutableStateOf(initial.done) }
    var invalid by retain(initial) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorialHeader(if (initial.id.isEmpty()) "Nuevo recordatorio" else "Editar recordatorio")
        OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
            isError = title.length > 512 || (invalid && title.isBlank()), supportingText = { Text("${title.length}/512") })
        DateField("Fecha", date, enabled = !busy) { date = it }
        TimeField(time, enabled = !busy) { time = it }
        TextButton(enabled = !busy && time.isNotEmpty(), onClick = { time = "" }) { Text("Todo el día · quitar hora") }
        Text("Tipo", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderKind.entries.forEach { option -> FilterChip(kind == option, { kind = option }, enabled = !busy, label = { Text(option.label()) }) }
        }
        FilterChip(done, { done = !done }, enabled = !busy, label = { Text("Completado") })
        if (invalid) Text("Indica un título de hasta 512 caracteres y una fecha válida.", color = MaterialTheme.colorScheme.error)
        if (saveError) Text("No pudimos guardar el recordatorio. Tus cambios siguen aquí.", color = MaterialTheme.colorScheme.error)
        Button(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
            invalid = title.isBlank() || title.length > 512 || isoToUtcMillis(date) == null
            if (!invalid) onSave(initial.copy(title = title.trim(), date = date, time = time.ifEmpty { null }, kind = kind, done = done))
        }) { Text(if (busy) "Guardando…" else "Guardar recordatorio") }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = onCancel) { Text("Cancelar") }
    }
}
