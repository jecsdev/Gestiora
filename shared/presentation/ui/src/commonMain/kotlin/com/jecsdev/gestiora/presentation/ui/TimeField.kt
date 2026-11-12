package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeField(value: String, enabled: Boolean, onValueChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text("Hora", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(value) }
    }
    if (open) {
        val state = rememberTimePickerState(
            initialHour = value.take(2).toIntOrNull()?.coerceIn(0, 23) ?: 9,
            initialMinute = value.takeLast(2).toIntOrNull()?.coerceIn(0, 59) ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Selecciona una hora") },
            text = { TimeInput(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange("${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}")
                    open = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        )
    }
}
