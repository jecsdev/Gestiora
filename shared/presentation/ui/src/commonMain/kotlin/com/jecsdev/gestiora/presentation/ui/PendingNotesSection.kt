package com.jecsdev.gestiora.presentation.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jecsdev.gestiora.domain.models.PrenatalNote
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalNoteViewModel

@Composable
internal fun PendingNotesSection(viewModel: PrenatalNoteViewModel, today: String, onOpen: () -> Unit) {
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
    EditorialSection("Seguimiento pendiente", "Ver tareas", onAction = onOpen) {
        when {
            failed -> {
                Text("No pudimos leer tus tareas. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { attempt++ }) { Text("Reintentar tareas") }
            }
            notes == null -> CircularProgressIndicator()
            notes!!.isEmpty() -> Text("No tienes tareas pendientes con fecha de seguimiento.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> notes!!.forEach { note ->
                val date = requireNotNull(note.followUp)
                EditorialFollowUpRow(
                    title = note.title,
                    supporting = when {
                        date < today -> "Fecha pasada: ${dateLabel(date)} · sigue pendiente"
                        date == today -> "Seguimiento hoy"
                        else -> "Seguimiento: ${dateLabel(date)}"
                    },
                    onClick = onOpen,
                )
            }
        }
    }
}
