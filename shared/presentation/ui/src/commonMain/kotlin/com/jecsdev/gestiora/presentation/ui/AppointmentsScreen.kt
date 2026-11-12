package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.Appointment
import com.jecsdev.gestiora.domain.models.AppointmentStatus
import com.jecsdev.gestiora.domain.models.AppointmentReminder
import com.jecsdev.gestiora.presentation.viewmodels.AppointmentViewModel
import kotlinx.coroutines.launch

private fun AppointmentStatus.label(): String = when (this) {
    AppointmentStatus.SCHEDULED -> "Programada"
    AppointmentStatus.COMPLETED -> "Completada"
    AppointmentStatus.CANCELLED -> "Cancelada"
}

private fun AppointmentReminder.label(): String = when (this) {
    AppointmentReminder.NONE -> "Sin recordatorio"
    AppointmentReminder.ONE_HOUR -> "1 hora antes"
    AppointmentReminder.ONE_DAY -> "1 día antes"
    AppointmentReminder.TWO_DAYS -> "2 días antes"
    AppointmentReminder.ONE_WEEK -> "1 semana antes"
}

@Composable
internal fun AppointmentsScreen(viewModel: AppointmentViewModel, initialDate: String?, onNavigationLockChange: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var appointments by remember { mutableStateOf<List<Appointment>?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var operationError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Appointment?>(null) }
    var deleting by remember { mutableStateOf<Appointment?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val today = todayIsoDate()
    val validInitialDate = initialDate?.takeIf { isoToUtcMillis(it) != null }
    var selectedDate by remember { mutableStateOf(validInitialDate.orEmpty()) }
    var displayedMonth by remember { mutableStateOf((validInitialDate ?: today).take(7)) }

    AppointmentBackHandler(enabled = busy || editing != null || deleting != null) {
        when {
            busy -> Unit
            confirmDiscard -> confirmDiscard = false
            deleting != null -> { deleting = null; onNavigationLockChange(false) }
            editing != null -> confirmDiscard = true
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("¿Descartar cambios?") },
            text = { Text("Los cambios que no hayas guardado se perderán.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    editing = null
                    operationError = false
                    onNavigationLockChange(false)
                }) { Text("Descartar") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Seguir editando") } },
        )
    }

    suspend fun reload() {
        val result = viewModel.load()
        appointments = result.getOrNull()?.sortedWith(compareBy(Appointment::date, Appointment::time))
        loadError = result.isFailure
    }

    LaunchedEffect(viewModel) { reload() }

    if (editing != null) {
        AppointmentForm(editing!!, busy, operationError, onCancel = { confirmDiscard = true }) { appointment ->
            busy = true
            scope.launch {
                try {
                    operationError = false
                    val result = if (appointment.id.isEmpty()) viewModel.add(appointment) else viewModel.update(appointment)
                    if (result.isSuccess) {
                        if (selectedDate.isNotEmpty()) {
                            selectedDate = appointment.date
                            displayedMonth = appointment.date.take(7)
                        }
                        editing = null
                        reload()
                    } else operationError = true
                } finally { busy = false; if (editing == null) onNavigationLockChange(false) }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Calendario", style = MaterialTheme.typography.headlineLarge)
        Text("Tus citas se guardan en este dispositivo.", style = MaterialTheme.typography.bodyMedium)
        when {
            loadError -> {
                Text("No pudimos leer tus citas. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch {
                        try { loadError = false; reload() } finally { busy = false; onNavigationLockChange(false) }
                    }
                }) { Text("Reintentar") }
            }
            appointments == null -> CircularProgressIndicator()
            else -> {
                AppointmentCalendar(
                    month = displayedMonth,
                    selectedDate = selectedDate,
                    today = today,
                    markedDates = appointments!!.map { it.date }.toSet(),
                    enabled = !busy,
                    onMonthChange = { displayedMonth = it },
                    onDateSelect = { selectedDate = it; displayedMonth = it.take(7) },
                )
                if (selectedDate.isNotEmpty()) {
                    Text("Citas del $selectedDate", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { selectedDate = "" }, enabled = !busy) { Text("Ver todas las citas") }
                } else {
                    Text("Todas las citas", style = MaterialTheme.typography.titleMedium)
                }
                Button(onClick = {
                    operationError = false
                    onNavigationLockChange(true)
                    editing = Appointment("", selectedDate.ifEmpty { todayIsoDate() }, "09:00", "", "", "", "", AppointmentStatus.SCHEDULED)
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Nueva cita") }
                val visibleAppointments = appointments!!.filter { selectedDate.isEmpty() || it.date == selectedDate }
                if (visibleAppointments.isEmpty()) {
                    Text(if (selectedDate.isEmpty()) "Aún no tienes citas registradas." else "No tienes citas para esta fecha.")
                }
                visibleAppointments.forEach { appointment ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${appointment.date} · ${appointment.time}", style = MaterialTheme.typography.titleMedium)
                            Text("${appointment.doctor} · ${appointment.specialty}")
                            Text(appointment.clinic)
                            Text(appointment.reason)
                            Text(appointment.status.label(), style = MaterialTheme.typography.labelMedium)
                            if (appointment.reminder != AppointmentReminder.NONE) {
                                Text("Preferencia: ${appointment.reminder.label()} · sin notificación", style = MaterialTheme.typography.bodySmall)
                            }
                            appointment.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { operationError = false; onNavigationLockChange(true); editing = appointment }, enabled = !busy) { Text("Editar") }
                                TextButton(onClick = { onNavigationLockChange(true); deleting = appointment }, enabled = !busy) { Text("Eliminar") }
                            }
                        }
                    }
                }
                if (operationError) Text("No pudimos eliminar la cita. Inténtalo de nuevo.", color = MaterialTheme.colorScheme.error)
                Text("Las preferencias de recordatorio se guardan; las notificaciones del sistema todavía no están disponibles.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    deleting?.let { appointment ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text("¿Eliminar cita?") },
            text = { Text("Se eliminará la cita del ${appointment.date} a las ${appointment.time} de este dispositivo.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            operationError = false
                            val result = viewModel.delete(appointment.id)
                            deleting = null
                            if (result.isSuccess) reload() else operationError = true
                        } finally { busy = false; onNavigationLockChange(false) }
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null; onNavigationLockChange(false) }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun AppointmentForm(initial: Appointment, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onSave: (Appointment) -> Unit) {
    var date by remember(initial) { mutableStateOf(initial.date) }
    var time by remember(initial) { mutableStateOf(initial.time) }
    var doctor by remember(initial) { mutableStateOf(initial.doctor) }
    var specialty by remember(initial) { mutableStateOf(initial.specialty) }
    var clinic by remember(initial) { mutableStateOf(initial.clinic) }
    var reason by remember(initial) { mutableStateOf(initial.reason) }
    var notes by remember(initial) { mutableStateOf(initial.notes.orEmpty()) }
    var status by remember(initial) { mutableStateOf(initial.status) }
    var reminder by remember(initial) { mutableStateOf(initial.reminder) }
    var invalid by remember(initial) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (initial.id.isEmpty()) "Nueva cita" else "Editar cita", style = MaterialTheme.typography.headlineLarge)
        DateField("Fecha", date, enabled = !busy) { date = it }
        TimeField(time, enabled = !busy) { time = it }
        AppointmentTextField("Médico/a", doctor, 512, !busy, true) { doctor = it }
        AppointmentTextField("Especialidad", specialty, 512, !busy, true) { specialty = it }
        AppointmentTextField("Clínica", clinic, 512, !busy, true) { clinic = it }
        AppointmentTextField("Motivo", reason, 512, !busy, false) { reason = it }
        AppointmentTextField("Notas (opcional)", notes, 4096, !busy, false) { notes = it }
        Text("Estado", style = MaterialTheme.typography.labelLarge)
        AppointmentStatus.entries.forEach { option -> FilterChip(selected = status == option, enabled = !busy, onClick = { status = option }, label = { Text(option.label()) }) }
        Text("Preferencia de recordatorio", style = MaterialTheme.typography.labelLarge)
        Text("Esta opción solo guarda tu preferencia. No se enviará una notificación.", style = MaterialTheme.typography.bodySmall)
        AppointmentReminder.entries.forEach { option ->
            FilterChip(selected = reminder == option, enabled = !busy, onClick = { reminder = option }, label = { Text(option.label()) })
        }
        if (invalid) Text("Completa los campos, respeta sus límites y usa una hora válida en formato HH:mm.", color = MaterialTheme.colorScheme.error)
        if (saveError) Text("No pudimos guardar la cita. Revisa los campos e inténtalo de nuevo; el formulario sigue aquí.", color = MaterialTheme.colorScheme.error)
        Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
            val validTime = time.length == 5 && time[2] == ':' && time.substring(0, 2).toIntOrNull() in 0..23 && time.substring(3, 5).toIntOrNull() in 0..59 && time.filterIndexed { index, _ -> index != 2 }.all { it in '0'..'9' }
            invalid = isoToUtcMillis(date) == null || !validTime || listOf(doctor, specialty, clinic, reason).any { it.isBlank() || it.length > 512 } || notes.length > 4096
            if (!invalid) onSave(Appointment(initial.id, date, time, doctor.trim(), specialty.trim(), clinic.trim(), reason.trim(), status, notes.trim().ifEmpty { null }, reminder))
        }) { Text(if (busy) "Guardando…" else "Guardar cita") }
        OutlinedButton(onClick = onCancel, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
    }
}

@Composable
private fun AppointmentTextField(label: String, value: String, limit: Int, enabled: Boolean, singleLine: Boolean, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = singleLine,
        isError = value.length > limit,
        supportingText = { Text(if (value.length > limit) "Máximo $limit caracteres (${value.length}/$limit)" else "${value.length}/$limit") },
        modifier = Modifier.fillMaxWidth(),
    )
}
