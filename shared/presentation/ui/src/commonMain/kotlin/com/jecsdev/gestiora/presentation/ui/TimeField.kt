package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeField(value: String, enabled: Boolean, onValueChange: (String) -> Unit) {
    val ui = LocalUiCopy.current
    var open by remember { mutableStateOf(false) }
    Column {
        Text(ui.s(Res.string.time), style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { open = true }, enabled = enabled, colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).semantics {
            contentDescription = ui.s(Res.string.time_accessibility, value.ifEmpty { ui.s(Res.string.all_day) })
        }) {
            Text(value.ifEmpty { ui.s(Res.string.all_day) }, modifier = Modifier.clearAndSetSemantics { })
        }
    }
    if (open) {
        val state = rememberTimePickerState(
            initialHour = value.take(2).toIntOrNull()?.coerceIn(0, 23) ?: 9,
            initialMinute = value.takeLast(2).toIntOrNull()?.coerceIn(0, 59) ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(ui.s(Res.string.select_a_time)) },
            text = { TimeInput(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange("${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}")
                    open = false
                }) { Text(ui.s(Res.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(ui.s(Res.string.cancel)) } },
        )
    }
}
