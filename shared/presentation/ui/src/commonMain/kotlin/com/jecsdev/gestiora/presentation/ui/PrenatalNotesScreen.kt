package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.*
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalNoteViewModel
import kotlinx.coroutines.launch

private fun NoteCategory.label(ui: UiCopy) = when (this) {
    NoteCategory.MEDICAL_INSTRUCTIONS -> ui.s(Res.string.medical_instructions)
    NoteCategory.DOCTOR_QUESTIONS -> ui.s(Res.string.questions_for_the_doctor)
    NoteCategory.MEDICATIONS -> ui.s(Res.string.medications)
    NoteCategory.NUTRITION -> ui.s(Res.string.nutrition)
    NoteCategory.SYMPTOMS -> ui.s(Res.string.symptoms)
    NoteCategory.GENERAL -> ui.s(Res.string.general_notes)
}
private fun NoteTaskStatus.label(ui: UiCopy) = when (this) {
    NoteTaskStatus.NONE -> ui.s(Res.string.no_task)
    NoteTaskStatus.PENDING -> ui.s(Res.string.pending)
    NoteTaskStatus.COMPLETED -> ui.s(Res.string.completed)
}

@Composable
internal fun PrenatalNotesScreen(viewModel: PrenatalNoteViewModel, initialStatus: NoteTaskStatus? = null, onNavigationLockChange: (Boolean) -> Unit) {
    val ui = LocalUiCopy.current
    val scope = retainedOperationScope()
    var notes by retain { mutableStateOf<List<PrenatalNote>?>(null) }
    var loadError by retain { mutableStateOf(false) }
    var operationError by retain { mutableStateOf(false) }
    var busy by retain { mutableStateOf(false) }
    var editing by retain { mutableStateOf<PrenatalNote?>(null) }
    var deleting by retain { mutableStateOf<PrenatalNote?>(null) }
    var discard by retain { mutableStateOf(false) }
    var query by retain { mutableStateOf("") }
    var statusFilter by retain { mutableStateOf(initialStatus) }
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
        title = { Text(ui.s(Res.string.discard_changes)) },
        text = { Text(ui.s(Res.string.changes_you_haven_t_saved_will_be_lost)) },
        confirmButton = { TextButton(onClick = { discard = false; editing = null; operationError = false; onNavigationLockChange(false) }) { Text(ui.s(Res.string.discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(ui.s(Res.string.keep_editing)) } },
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
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        EditorialHeader(ui.s(Res.string.notes_and_follow_up), ui.s(Res.string.your_notes_are_saved_on_this_device), compact = true)
        when {
            loadError -> {
                Text(ui.s(Res.string.we_couldn_t_read_your_notes_your_data), color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text(ui.s(Res.string.retry)) }
            }
            notes == null -> CircularProgressIndicator()
            else -> {
                OutlinedTextField(query, { query = it }, label = { Text(ui.s(Res.string.search_title_and_content)) }, singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth())
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(statusFilter == null, { statusFilter = null }, enabled = !busy, label = { Text(ui.s(Res.string.all)) })
                    NoteTaskStatus.entries.forEach { option ->
                        FilterChip(statusFilter == option, { statusFilter = option }, enabled = !busy, label = { Text(option.label(ui)) })
                    }
                }
                Button(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = { operationError = false; editing = PrenatalNote("", ""); onNavigationLockChange(true) }) { Text(ui.s(Res.string.new_note)) }
                val visibleNotes = filterNotes(notes!!, query, statusFilter)
                if (notes!!.isEmpty()) Text(ui.s(Res.string.you_haven_t_recorded_any_notes_yet))
                else if (visibleNotes.isEmpty()) Text(ui.s(Res.string.no_notes_match_these_filters))
                if (query.isNotEmpty() || statusFilter != null) TextButton(enabled = !busy, onClick = { query = ""; statusFilter = null }) { Text(ui.s(Res.string.clear_filters)) }
                visibleNotes.forEach { note ->
                    EditorialRecordCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(note.title, style = MaterialTheme.typography.titleMedium)
                            Text(note.category.label(ui), style = MaterialTheme.typography.labelMedium)
                            note.body?.let { Text(it) }
                            EditorialStatus(note.status.label(ui))
                            note.followUp?.let { Text(ui.s(Res.string.follow_up_3, dateLabel(ui, it))) }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled = !busy, onClick = { operationError = false; editing = note; onNavigationLockChange(true) }) { Text(ui.s(Res.string.edit)) }
                                TextButton(enabled = !busy, onClick = { deleting = note; onNavigationLockChange(true) }) { Text(ui.s(Res.string.delete)) }
                            }
                        }
                    }
                }
                if (operationError) Text(ui.s(Res.string.we_couldn_t_delete_the_note_try_again), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { note ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text(ui.s(Res.string.delete_note)) },
            text = { Text(ui.s(Res.string.this_note_will_be_deleted_from_this_device)) },
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
            }) { Text(ui.s(Res.string.delete)) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null; onNavigationLockChange(false) }) { Text(ui.s(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun PrenatalNoteForm(initial: PrenatalNote, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onSave: (PrenatalNote) -> Unit) {
    val ui = LocalUiCopy.current
    var title by retain(initial) { mutableStateOf(initial.title) }
    var body by retain(initial) { mutableStateOf(initial.body.orEmpty()) }
    var category by retain(initial) { mutableStateOf(initial.category) }
    var status by retain(initial) { mutableStateOf(initial.status) }
    var followUp by retain(initial) { mutableStateOf(initial.followUp.orEmpty()) }
    var invalid by retain(initial) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().screenEntrance().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorialHeader(if (initial.id.isEmpty()) ui.s(Res.string.new_note) else ui.s(Res.string.edit_note))
        OutlinedTextField(title, { title = it }, label = { Text(ui.s(Res.string.title)) }, singleLine = true, enabled = !busy, isError = title.length > 512, supportingText = { Text("${title.length}/512") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(body, { body = it }, label = { Text(ui.s(Res.string.content_optional)) }, enabled = !busy, isError = body.length > 4096, supportingText = { Text("${body.length}/4096") }, modifier = Modifier.fillMaxWidth())
        Text(ui.s(Res.string.category), style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoteCategory.entries.forEach { option -> FilterChip(category == option, { category = option }, enabled = !busy, label = { Text(option.label(ui)) }) }
        }
        Text(ui.s(Res.string.follow_up), style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NoteTaskStatus.entries.forEach { option -> FilterChip(status == option, { status = option }, enabled = !busy, label = { Text(option.label(ui)) }) }
        }
        DateField(ui.s(Res.string.follow_up_date_optional), followUp, clearable = true, enabled = !busy) { followUp = it }
        Text(ui.s(Res.string.the_follow_up_date_does_not_generate_a), style = MaterialTheme.typography.bodySmall)
        if (invalid) Text(ui.s(Res.string.enter_a_title_and_respect_the_field_limits), color = MaterialTheme.colorScheme.error)
        if (saveError) Text(ui.s(Res.string.we_couldn_t_save_the_note_your_changes), color = MaterialTheme.colorScheme.error)
        Button(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
            invalid = title.isBlank() || title.length > 512 || body.length > 4096 || (followUp.isNotEmpty() && isoToUtcMillis(followUp) == null)
            if (!invalid) onSave(initial.copy(title = title.trim(), body = body.trim().ifEmpty { null }, category = category, status = status, followUp = followUp.ifEmpty { null }))
        }) { Text(if (busy) ui.s(Res.string.saving) else ui.s(Res.string.save_note)) }
        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = onCancel) { Text(ui.s(Res.string.cancel)) }
    }
}
