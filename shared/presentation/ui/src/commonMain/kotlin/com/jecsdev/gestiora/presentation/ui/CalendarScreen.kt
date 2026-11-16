package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

@Composable
internal fun CalendarScreen(initialDate: String?, onNavigationLockChange: (Boolean) -> Unit) {
    var reminders by retain { mutableStateOf(false) }
    var locked by retain { mutableStateOf(false) }
    val lock: (Boolean) -> Unit = { locked = it; onNavigationLockChange(it) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!reminders, { reminders = false }, enabled = !locked, label = { Text("Citas") })
            FilterChip(reminders, { reminders = true }, enabled = !locked, label = { Text("Recordatorios") })
        }
        Box(Modifier.weight(1f)) {
            if (reminders) PersonalRemindersScreen(koinInject(), lock)
            else AppointmentsScreen(koinInject(), initialDate, lock)
        }
    }
}
