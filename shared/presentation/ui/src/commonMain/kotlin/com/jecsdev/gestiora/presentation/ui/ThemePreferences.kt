package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.core.ui.AppTheme
import com.jecsdev.gestiora.domain.models.ThemeMode
import com.jecsdev.gestiora.presentation.viewmodels.ThemePreferenceViewModel
import kotlinx.coroutines.launch

@Composable
internal fun ThemeRoot(
    viewModel: ThemePreferenceViewModel,
    content: @Composable (preferences: @Composable () -> Unit) -> Unit,
) {
    var mode by retain { mutableStateOf(ThemeMode.SYSTEM) }
    var loaded by retain { mutableStateOf(false) }
    var busy by retain { mutableStateOf(false) }
    var readFailed by retain { mutableStateOf(false) }
    var saveFailed by retain { mutableStateOf(false) }
    val scope = retainedOperationScope()

    suspend fun load() {
        busy = true
        try {
            val result = viewModel.load()
            result.onSuccess { mode = it }
            readFailed = result.isFailure
            loaded = true
        } finally { busy = false }
    }
    LaunchedEffect(viewModel) { if (!loaded) load() }

    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    AppTheme(darkTheme = dark) {
        SystemAppearance(dark, followSystem = mode == ThemeMode.SYSTEM)
        content {
            ThemePreferences(mode, !loaded || busy, readFailed, saveFailed,
                onRetry = {
                    if (!busy) scope.launch { load() }
                },
                onSelect = { selected ->
                    if (!busy && loaded && !readFailed) {
                        busy = true
                        saveFailed = false
                        scope.launch {
                            try {
                                val result = viewModel.save(selected)
                                if (result.isSuccess) mode = selected else saveFailed = true
                            } finally { busy = false }
                        }
                    }
                },
            )
        }
    }
}

@Composable
internal expect fun SystemAppearance(dark: Boolean, followSystem: Boolean)

@Composable
internal fun ThemePreferences(
    mode: ThemeMode,
    busy: Boolean,
    readFailed: Boolean,
    saveFailed: Boolean,
    onRetry: () -> Unit,
    onSelect: (ThemeMode) -> Unit,
) {
    val ui = LocalUiCopy.current
    EditorialSection(ui.s(Res.string.preferences)) {
        EditorialRecordCard {
            Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui.s(Res.string.appearance), style = MaterialTheme.typography.titleMedium)
                Text(ui.s(Res.string.choose_how_gestiora_looks), style = MaterialTheme.typography.bodyMedium)
                if (readFailed) {
                    Text(ui.s(Res.string.we_couldn_t_read_your_preference_we_are), color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry, enabled = !busy) { Text(ui.s(Res.string.retry)) }
                } else {
                    ThemeMode.entries.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).selectable(
                                selected = mode == option,
                                enabled = !busy,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            ).padding(vertical = 4.dp).heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = mode == option, onClick = null, enabled = !busy)
                            Text(when (option) {
                                ThemeMode.SYSTEM -> ui.s(Res.string.follow_system)
                                ThemeMode.LIGHT -> ui.s(Res.string.light)
                                ThemeMode.DARK -> ui.s(Res.string.dark)
                            })
                        }
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (saveFailed) Text(ui.s(Res.string.we_couldn_t_save_the_appearance_select_it), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
