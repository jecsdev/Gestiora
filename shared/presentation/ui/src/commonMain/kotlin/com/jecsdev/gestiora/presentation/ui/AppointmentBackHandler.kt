package com.jecsdev.gestiora.presentation.ui

import androidx.compose.runtime.Composable

@Composable
internal expect fun AppointmentBackHandler(enabled: Boolean, onBack: () -> Unit)
