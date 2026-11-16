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
import com.jecsdev.gestiora.domain.models.PersonalReminder
import com.jecsdev.gestiora.domain.models.ReminderKind
import com.jecsdev.gestiora.presentation.viewmodels.PersonalReminderViewModel
import kotlinx.coroutines.launch

private fun ReminderKind.label(ui: UiCopy) = when (this) {
    ReminderKind.APPOINTMENT -> ui.s(Res.string.appointment)
    ReminderKind.EXAM -> ui.s(Res.string.examination)
    ReminderKind.VACCINE -> ui.s(Res.string.vaccine)
    ReminderKind.FOLLOW_UP -> ui.s(Res.string.follow_up)
    ReminderKind.PERSONAL -> ui.s(Res.string.personal)
}

@Composable
internal fun PersonalRemindersScreen(viewModel: PersonalReminderViewModel, onNavigationLockChange: (Boolean) -> Unit) {
    val ui = LocalUiCopy.current
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
        onDismissRequest = { discard = false }, title = { Text(ui.s(Res.string.discard_changes)) },
        text = { Text(ui.s(Res.string.changes_you_haven_t_saved_will_be_lost)) },
        confirmButton = { TextButton(onClick = { discard = false; editing = null; saveError = false; onNavigationLockChange(false) }) { Text(ui.s(Res.string.discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(ui.s(Res.string.keep_editing)) } },
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
        EditorialHeader(ui.s(Res.string.reminders), ui.s(Res.string.organize_your_tasks_on_this_device_reminders_appear))
        when {
            loadError -> {
                Text(ui.s(Res.string.we_couldn_t_read_your_reminders_your_data), color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true; onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text(ui.s(Res.string.retry)) }
            }
            records == null -> CircularProgressIndicator()
            else -> {
                AppointmentCalendar(month, selectedDate, todayIsoDate(), records!!.map { it.date }.toSet(), !busy,
                    onMonthChange = { month = it }, onDateSelect = { selectedDate = it; month = it.take(7) }, markedDescription = ui.s(Res.string.has_reminders))
                Button(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
                    saveError = false; editing = PersonalReminder("", "", selectedDate.ifEmpty { todayIsoDate() }); onNavigationLockChange(true)
                }) { Text(ui.s(Res.string.new_reminder)) }
                if (selectedDate.isNotEmpty()) {
                    Text(ui.s(Res.string.reminders_on, dateLabel(ui, selectedDate)), style = MaterialTheme.typography.titleMedium)
                    TextButton(enabled = !busy, onClick = { selectedDate = "" }) { Text(ui.s(Res.string.view_all_reminders)) }
                }
                val visible = records!!.filter { selectedDate.isEmpty() || it.date == selectedDate }
                    .sortedWith(compareBy<PersonalReminder> { it.date }.thenBy { it.time.orEmpty() }.thenBy { it.id })
                if (visible.isEmpty()) Text(ui.s(Res.string.you_have_no_reminders_for_this_selection), color = MaterialTheme.colorScheme.onSurfaceVariant)
                visible.forEach { record ->
                    EditorialRecordCard {
                        EditorialStatus(if (record.done) ui.s(Res.string.completed_2) else record.kind.label(ui))
                        Text(record.title, style = MaterialTheme.typography.titleMedium)
                        Text(ui.s(Res.string.reminder_date_time, dateLabel(ui, record.date), record.time ?: ui.s(Res.string.all_day)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(enabled = !busy, onClick = { saveError = false; editing = record; onNavigationLockChange(true) }) { Text(ui.s(Res.string.edit)) }
                            TextButton(enabled = !busy, onClick = { deleting = record; onNavigationLockChange(true) }) { Text(ui.s(Res.string.delete)) }
                        }
                    }
                }
                if (saveError) Text(ui.s(Res.string.we_couldn_t_delete_the_reminder_try_again), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { record ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text(ui.s(Res.string.delete_reminder)) }, text = { Text(ui.s(Res.string.this_reminder_will_be_deleted_from_this_device)) },
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
            }) { Text(ui.s(Res.string.delete)) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null; onNavigationLockChange(false) }) { Text(ui.s(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun PersonalReminderForm(initial: PersonalReminder, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onSave: (PersonalReminder) -> Unit) {
    val ui = LocalUiCopy.current
    var title by retain(initial) { mutableStateOf(initial.title) }
    var date by retain(initial) { mutableStateOf(initial.date) }
    var time by retain(initial) { mutableStateOf(initial.time.orEmpty()) }
    var kind by retain(initial) { mutableStateOf(initial.kind) }
    var done by retain(initial) { mutableStateOf(initial.done) }
    var invalid by retain(initial) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().screenEntrance().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorialHeader(if (initial.id.isEmpty()) ui.s(Res.string.new_reminder) else ui.s(Res.string.edit_reminder))
        OutlinedTextField(title, { title = it }, label = { Text(ui.s(Res.string.title)) }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
            isError = title.length > 512 || (invalid && title.isBlank()), supportingText = { Text("${title.length}/512") })
        DateField(ui.s(Res.string.date), date, enabled = !busy) { date = it }
        TimeField(time, enabled = !busy) { time = it }
        TextButton(enabled = !busy && time.isNotEmpty(), onClick = { time = "" }) { Text(ui.s(Res.string.all_day_remove_time)) }
        Text(ui.s(Res.string.type), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderKind.entries.forEach { option -> FilterChip(kind == option, { kind = option }, enabled = !busy, label = { Text(option.label(ui)) }) }
        }
        FilterChip(done, { done = !done }, enabled = !busy, label = { Text(ui.s(Res.string.completed_2)) })
        if (invalid) Text(ui.s(Res.string.enter_a_title_of_up_to_512_characters), color = MaterialTheme.colorScheme.error)
        if (saveError) Text(ui.s(Res.string.we_couldn_t_save_the_reminder_your_changes), color = MaterialTheme.colorScheme.error)
        Button(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
            invalid = title.isBlank() || title.length > 512 || isoToUtcMillis(date) == null
            if (!invalid) onSave(initial.copy(title = title.trim(), date = date, time = time.ifEmpty { null }, kind = kind, done = done))
        }) { Text(if (busy) ui.s(Res.string.saving) else ui.s(Res.string.save_reminder)) }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = onCancel) { Text(ui.s(Res.string.cancel)) }
    }
}
