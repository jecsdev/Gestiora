package com.jecsdev.gestiora.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
internal actual fun AppointmentBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val leaving = LocalScreenLeaving.current
    BackHandler(enabled = enabled) { if (!leaving) onBack() }
}
