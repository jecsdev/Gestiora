package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.PregnancyJournalEntry
import gestiora.shared.presentation.ui.generated.resources.*
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class PregnancyJournalControls(
    val load: suspend () -> List<PregnancyJournalEntry>,
    val save: suspend (PregnancyJournalEntry) -> PregnancyJournalEntry,
    val delete: suspend (String) -> Unit,
)

internal val LocalPregnancyJournal = staticCompositionLocalOf<PregnancyJournalControls?> { null }

@Composable
internal fun PregnancyJournalRoute(onBack: () -> Unit) {
    val controls = LocalPregnancyJournal.current ?: return
    val ui = LocalUiCopy.current
    val operations = retainedOperationScope()
    var entries by retain { mutableStateOf<List<PregnancyJournalEntry>?>(null) }
    var loadFailed by retain { mutableStateOf(false) }
    var actionFailed by retain { mutableStateOf(false) }
    var opened by retain { mutableStateOf<PregnancyJournalEntry?>(null) }
    var editing by retain { mutableStateOf<PregnancyJournalEntry?>(null) }
    var confirmDelete by retain { mutableStateOf(false) }
    var busy by retain { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(controls, attempt) {
        if (entries == null) {
            loadFailed = false
            try {
                entries = controls.load()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                loadFailed = true
            }
        }
    }

    fun changeEntry(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        actionFailed = false
        operations.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                actionFailed = true
            } finally {
                busy = false
            }
        }
    }

    val closeEntry = { opened = null; actionFailed = false }
    AppointmentBackHandler(opened != null || busy) { if (!busy) closeEntry() }
    val listState = rememberLazyListState()
    ScreenExchange(opened, animate = { from, to -> (from == null) != (to == null) }) { entry ->
        if (entry == null) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { ScreenBackButton(onBack) }
                item { EditorialHeader(ui.s(Res.string.premium_journal), ui.s(Res.string.journal_private_space)) }
                when {
                    loadFailed -> item { PrenatalLoadError { attempt++ } }
                    entries == null -> item { CircularProgressIndicator() }
                    else -> {
                        item {
                            GradientButton(
                                onClick = { editing = PregnancyJournalEntry("", todayIsoDate(), "", "") },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(ui.s(Res.string.journal_new_entry)) }
                        }
                        val ordered = entries.orEmpty().asReversed().sortedByDescending { it.date }
                        if (ordered.isEmpty()) item { EditorialEmptyState(ui.s(Res.string.journal_empty)) }
                        items(ordered, key = { it.id }) { item ->
                            EditorialRecordCard {
                                Text(dateLabel(ui, item.date), style = MaterialTheme.typography.labelMedium)
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                if (item.body.isNotBlank()) Text(item.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                TextButton(onClick = { opened = item }) { Text(ui.s(Res.string.journal_open_entry)) }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ScreenBackButton(onBack = closeEntry, enabled = !busy)
                EditorialHeader(entry.title, dateLabel(ui, entry.date, expanded = true))
                EditorialRecordCard {
                    if (entry.body.isNotBlank()) Text(entry.body)
                    ClinicalAttachmentActions(ClinicalAttachmentKind.Journal, entry.id, !busy)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = !busy, onClick = { actionFailed = false; editing = entry }) { Text(ui.s(Res.string.edit)) }
                    TextButton(enabled = !busy, onClick = { actionFailed = false; confirmDelete = true }) { Text(ui.s(Res.string.delete)) }
                }
                if (actionFailed && editing == null) {
                    Text(ui.s(Res.string.journal_update_failed), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    editing?.let { initial ->
        RecordEditorSheet(busy = busy, onClose = { editing = null; actionFailed = false }) { cancel, onChanges ->
            JournalEntryForm(initial, busy, actionFailed, cancel, onChanges) { draft ->
                changeEntry {
                    val saved = controls.save(draft)
                    entries = entries.orEmpty().filterNot { it.id == saved.id } + saved
                    editing = null
                    opened = saved
                }
            }
        }
    }
    val entry = opened
    if (confirmDelete && entry != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmDelete = false },
            title = { Text(ui.s(Res.string.journal_delete_entry)) },
            text = { Text(ui.s(Res.string.journal_delete_detail)) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    confirmDelete = false
                    changeEntry {
                        controls.delete(entry.id)
                        entries = entries.orEmpty().filterNot { it.id == entry.id }
                        closeEntry()
                    }
                }) { Text(ui.s(Res.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(ui.s(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun JournalEntryForm(
    initial: PregnancyJournalEntry,
    busy: Boolean,
    failed: Boolean,
    onCancel: () -> Unit,
    onChanges: (Boolean) -> Unit,
    onSave: (PregnancyJournalEntry) -> Unit,
) {
    val ui = LocalUiCopy.current
    var title by retain(initial) { mutableStateOf(initial.title) }
    var body by retain(initial) { mutableStateOf(initial.body) }
    var date by retain(initial) { mutableStateOf(initial.date) }
    SideEffect { onChanges(title != initial.title || body != initial.body || date != initial.date) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditorialHeader(ui.s(if (initial.id.isEmpty()) Res.string.journal_new_entry else Res.string.journal_edit_entry))
        DateField(ui.s(Res.string.date), date, enabled = !busy) { date = it }
        PastelTextField(
            title, { if (it.length <= PregnancyJournalEntry.MAX_TITLE) title = it },
            label = { Text(ui.s(Res.string.title)) }, singleLine = true, enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        PastelTextField(
            body, { if (it.length <= PregnancyJournalEntry.MAX_BODY) body = it },
            label = { Text(ui.s(Res.string.content_optional)) }, enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        if (failed) Text(ui.s(Res.string.journal_update_failed), color = MaterialTheme.colorScheme.error)
        GradientButton(
            modifier = Modifier.fillMaxWidth(), enabled = !busy && title.isNotBlank(),
            onClick = { onSave(initial.copy(date = date, title = title.trim(), body = body.trim())) },
        ) { Text(ui.s(Res.string.journal_save_entry)) }
        TextButton(onClick = onCancel, enabled = !busy) { Text(ui.s(Res.string.cancel)) }
    }
}
