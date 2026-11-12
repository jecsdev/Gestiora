package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    Text("Seguimiento pendiente", style = MaterialTheme.typography.titleLarge)
    when {
        failed -> {
            Text("No pudimos leer tus tareas. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { attempt++ }) { Text("Reintentar tareas") }
        }
        notes == null -> CircularProgressIndicator()
        notes!!.isEmpty() -> Text("No tienes tareas pendientes con fecha de seguimiento.")
        else -> notes!!.forEach { note ->
            Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(note.title, style = MaterialTheme.typography.titleMedium)
                    val date = requireNotNull(note.followUp)
                    Text(when {
                        date < today -> "Fecha pasada: $date · sigue pendiente"
                        date == today -> "Seguimiento hoy"
                        else -> "Seguimiento: $date"
                    }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("Ver tareas pendientes") }
}
