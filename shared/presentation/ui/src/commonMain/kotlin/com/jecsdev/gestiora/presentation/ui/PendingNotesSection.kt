package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jecsdev.gestiora.domain.models.PrenatalNote
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalNoteViewModel

@Composable
internal fun PendingNotesSection(viewModel: PrenatalNoteViewModel, today: String, onOpen: () -> Unit) {
    val ui = LocalUiCopy.current
    var notes by remember(viewModel) { mutableStateOf<List<PrenatalNote>?>(null) }
    var failed by remember(viewModel) { mutableStateOf(false) }
    var attempt by remember(viewModel) { mutableStateOf(0) }
    LaunchedEffect(viewModel, attempt) {
        failed = false
        notes = null
        val result = viewModel.load()
        notes = result.getOrNull()?.let(::datedPendingNotes)
        failed = result.isFailure
    }
    EditorialSection(ui.s(Res.string.pending_follow_up), ui.s(Res.string.view_tasks), onAction = onOpen) {
        when {
            failed -> {
                Text(ui.s(Res.string.we_couldn_t_read_your_tasks_your_data), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { attempt++ }) { Text(ui.s(Res.string.retry_tasks)) }
            }
            notes == null -> CircularProgressIndicator()
            notes!!.isEmpty() -> Text(ui.s(Res.string.you_have_no_pending_tasks_with_a_follow), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> notes!!.forEach { note ->
                val date = requireNotNull(note.followUp)
                EditorialFollowUpRow(
                    title = note.title,
                    supporting = when {
                        date < today -> ui.s(Res.string.past_date_still_pending, dateLabel(ui, date))
                        date == today -> ui.s(Res.string.follow_up_today)
                        else -> ui.s(Res.string.follow_up_2, dateLabel(ui, date))
                    },
                    onClick = onOpen,
                )
            }
        }
    }
}
