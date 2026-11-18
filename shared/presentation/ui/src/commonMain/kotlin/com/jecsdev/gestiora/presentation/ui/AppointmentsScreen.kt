package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.Appointment
import com.jecsdev.gestiora.domain.models.AppointmentStatus
import com.jecsdev.gestiora.domain.models.AppointmentReminder
import com.jecsdev.gestiora.presentation.viewmodels.AppointmentViewModel
import kotlinx.coroutines.launch

private fun AppointmentStatus.label(ui: UiCopy): String = when (this) {
    AppointmentStatus.SCHEDULED -> ui.s(Res.string.scheduled)
    AppointmentStatus.COMPLETED -> ui.s(Res.string.completed)
    AppointmentStatus.CANCELLED -> ui.s(Res.string.cancelled)
}

private fun AppointmentReminder.label(ui: UiCopy): String = when (this) {
    AppointmentReminder.NONE -> ui.s(Res.string.no_reminder)
    AppointmentReminder.ONE_HOUR -> ui.s(Res.string.text_1_hour_before)
    AppointmentReminder.ONE_DAY -> ui.s(Res.string.text_1_day_before)
    AppointmentReminder.TWO_DAYS -> ui.s(Res.string.text_2_days_before)
    AppointmentReminder.ONE_WEEK -> ui.s(Res.string.text_1_week_before)
}

@Composable
internal fun AppointmentsScreen(viewModel: AppointmentViewModel, initialDate: String?, onNavigationLockChange: (Boolean) -> Unit) {
    val ui = LocalUiCopy.current
    val scope = retainedOperationScope()
    var appointments by retain { mutableStateOf<List<Appointment>?>(null) }
    var loadError by retain { mutableStateOf(false) }
    var operationError by retain { mutableStateOf(false) }
    var busy by retain { mutableStateOf(false) }
    var editing by retain { mutableStateOf<Appointment?>(null) }
    var deleting by retain { mutableStateOf<Appointment?>(null) }
    var confirmDiscard by retain { mutableStateOf(false) }
    val today = todayIsoDate()
    val validInitialDate = initialDate?.takeIf { isoToUtcMillis(it) != null }
    var selectedDate by retain { mutableStateOf(validInitialDate.orEmpty()) }
    var displayedMonth by retain { mutableStateOf((validInitialDate ?: today).take(7)) }

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
            title = { Text(ui.s(Res.string.discard_changes)) },
            text = { Text(ui.s(Res.string.changes_you_haven_t_saved_will_be_lost)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    editing = null
                    operationError = false
                    onNavigationLockChange(false)
                }) { Text(ui.s(Res.string.discard)) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(ui.s(Res.string.keep_editing)) } },
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

    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        EditorialHeader(ui.s(Res.string.calendar), ui.s(Res.string.your_appointments_are_saved_on_this_device))
        when {
            loadError -> {
                Text(ui.s(Res.string.we_couldn_t_read_your_appointments_your_data), color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch {
                        try { loadError = false; reload() } finally { busy = false; onNavigationLockChange(false) }
                    }
                }) { Text(ui.s(Res.string.retry)) }
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
                    Text(ui.s(Res.string.appointments_on, dateLabel(ui, selectedDate)), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { selectedDate = "" }, enabled = !busy) { Text(ui.s(Res.string.view_all_appointments)) }
                } else {
                    Text(ui.s(Res.string.all_appointments), style = MaterialTheme.typography.titleMedium)
                }
                Button(onClick = {
                    operationError = false
                    onNavigationLockChange(true)
                    editing = Appointment("", selectedDate.ifEmpty { todayIsoDate() }, "09:00", "", "", "", "", AppointmentStatus.SCHEDULED)
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(ui.s(Res.string.new_appointment)) }
                val visibleAppointments = appointments!!.filter { selectedDate.isEmpty() || it.date == selectedDate }
                if (visibleAppointments.isEmpty()) {
                    Text(if (selectedDate.isEmpty()) ui.s(Res.string.you_haven_t_recorded_any_appointments_yet) else ui.s(Res.string.you_have_no_appointments_on_this_date))
                }
                visibleAppointments.forEach { appointment ->
                    EditorialRecordCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(ui.s(Res.string.text_, dateLabel(ui, appointment.date), appointment.time), style = MaterialTheme.typography.titleMedium)
                            Text(ui.s(Res.string.text__2, appointment.doctor, appointment.specialty))
                            Text(appointment.clinic)
                            Text(appointment.reason)
                            EditorialStatus(appointment.status.label(ui))
                            if (appointment.reminder != AppointmentReminder.NONE) {
                                Text(appointment.reminder.label(ui), style = MaterialTheme.typography.bodySmall)
                            }
                            appointment.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { operationError = false; onNavigationLockChange(true); editing = appointment }, enabled = !busy) { Text(ui.s(Res.string.edit)) }
                                TextButton(onClick = { onNavigationLockChange(true); deleting = appointment }, enabled = !busy) { Text(ui.s(Res.string.delete)) }
                            }
                        }
                    }
                }
                if (operationError) Text(ui.s(Res.string.we_couldn_t_delete_the_appointment_try_again), color = MaterialTheme.colorScheme.error)
                ReminderNotificationSettings()
            }
        }
    }
    deleting?.let { appointment ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text(ui.s(Res.string.delete_appointment)) },
            text = { Text(ui.s(Res.string.the_appointment_on_at_will_be_deleted_from, dateLabel(ui, appointment.date), appointment.time)) },
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
                }) { Text(ui.s(Res.string.delete)) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null; onNavigationLockChange(false) }) { Text(ui.s(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun AppointmentForm(initial: Appointment, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onSave: (Appointment) -> Unit) {
    val ui = LocalUiCopy.current
    var date by retain(initial) { mutableStateOf(initial.date) }
    var time by retain(initial) { mutableStateOf(initial.time) }
    var doctor by retain(initial) { mutableStateOf(initial.doctor) }
    var specialty by retain(initial) { mutableStateOf(initial.specialty) }
    var clinic by retain(initial) { mutableStateOf(initial.clinic) }
    var reason by retain(initial) { mutableStateOf(initial.reason) }
    var notes by retain(initial) { mutableStateOf(initial.notes.orEmpty()) }
    var status by retain(initial) { mutableStateOf(initial.status) }
    var reminder by retain(initial) { mutableStateOf(initial.reminder) }
    var invalid by retain(initial) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().screenEntrance().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorialHeader(if (initial.id.isEmpty()) ui.s(Res.string.new_appointment) else ui.s(Res.string.edit_appointment))
        DateField(ui.s(Res.string.date), date, enabled = !busy) { date = it }
        TimeField(time, enabled = !busy) { time = it }
        AppointmentTextField(ui.s(Res.string.doctor), doctor, 512, !busy, true) { doctor = it }
        AppointmentTextField(ui.s(Res.string.specialty), specialty, 512, !busy, true) { specialty = it }
        AppointmentTextField(ui.s(Res.string.clinic), clinic, 512, !busy, true) { clinic = it }
        AppointmentTextField(ui.s(Res.string.reason), reason, 512, !busy, false) { reason = it }
        AppointmentTextField(ui.s(Res.string.notes_optional), notes, 4096, !busy, false) { notes = it }
        Text(ui.s(Res.string.status), style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppointmentStatus.entries.forEach { option -> FilterChip(selected = status == option, enabled = !busy, onClick = { status = option }, label = { Text(option.label(ui)) }) }
        }
        Text(ui.s(Res.string.reminder_preference), style = MaterialTheme.typography.labelLarge)
        ReminderNotificationSettings()
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppointmentReminder.entries.forEach { option ->
                FilterChip(selected = reminder == option, enabled = !busy, onClick = { reminder = option }, label = { Text(option.label(ui)) })
            }
        }
        if (invalid) Text(ui.s(Res.string.complete_the_fields_respect_their_limits_and_enter), color = MaterialTheme.colorScheme.error)
        if (saveError) Text(ui.s(Res.string.we_couldn_t_save_the_appointment_check_the), color = MaterialTheme.colorScheme.error)
        Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
            val validTime = time.length == 5 && time[2] == ':' && time.substring(0, 2).toIntOrNull() in 0..23 && time.substring(3, 5).toIntOrNull() in 0..59 && time.filterIndexed { index, _ -> index != 2 }.all { it in '0'..'9' }
            invalid = isoToUtcMillis(date) == null || !validTime || listOf(doctor, specialty, clinic, reason).any { it.isBlank() || it.length > 512 } || notes.length > 4096
            if (!invalid) onSave(Appointment(initial.id, date, time, doctor.trim(), specialty.trim(), clinic.trim(), reason.trim(), status, notes.trim().ifEmpty { null }, reminder))
        }) { Text(if (busy) ui.s(Res.string.saving) else ui.s(Res.string.save_appointment)) }
        OutlinedButton(onClick = onCancel, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(ui.s(Res.string.cancel)) }
    }
}

@Composable
private fun AppointmentTextField(label: String, value: String, limit: Int, enabled: Boolean, singleLine: Boolean, onValueChange: (String) -> Unit) {
    val ui = LocalUiCopy.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = singleLine,
        isError = value.length > limit,
        supportingText = { Text(if (value.length > limit) ui.s(Res.string.maximum_characters, limit, value.length, limit) else "${value.length}/$limit") },
        modifier = Modifier.fillMaxWidth(),
    )
}
