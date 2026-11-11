package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.core.di.CoreDependencyInjection
import com.jecsdev.gestiora.core.ui.AppTheme
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.presentation.viewmodels.ProfileLoad
import com.jecsdev.gestiora.presentation.viewmodels.ProfileViewModel
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.Module
import org.koin.dsl.koinConfiguration

@Composable
fun App(platformModules: List<Module> = emptyList()) {
    KoinApplication(configuration = koinConfiguration { modules(CoreDependencyInjection.modules + platformModules) }) {
        AppTheme { ProfileRoot(koinInject()) }
    }
}

@Composable
private fun ProfileRoot(viewModel: ProfileViewModel) {
    var loadAttempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<ProfileLoad>(ProfileLoad.Loading) }
    var editing by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<PregnancyProfile?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, loadAttempt) {
        state = ProfileLoad.Loading
        state = viewModel.load()
    }
    LaunchedEffect(pendingSave) {
        val profile = pendingSave ?: return@LaunchedEffect
        saving = true
        if (viewModel.save(profile)) {
            state = ProfileLoad.Ready(profile)
            editing = false
            saveFailed = false
        } else {
            saveFailed = true
        }
        saving = false
        pendingSave = null
    }

    when (val current = state) {
        ProfileLoad.Loading -> CenterMessage { CircularProgressIndicator() }
        ProfileLoad.Error -> CenterMessage {
            Text("No pudimos leer tu perfil. Tus datos no se han borrado.", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { loadAttempt++ }) { Text("Reintentar") }
        }
        ProfileLoad.Empty -> OnboardingScreen(
            initialProfile = null,
            viewModel = viewModel,
            saving = saving,
            saveFailed = saveFailed,
            onSave = { saveFailed = false; pendingSave = it },
        )
        is ProfileLoad.Ready -> if (editing) {
            OnboardingScreen(
                initialProfile = current.profile,
                viewModel = viewModel,
                saving = saving,
                saveFailed = saveFailed,
                onSave = { saveFailed = false; pendingSave = it },
                onCancel = { editing = false; saveFailed = false },
            )
        } else {
            PregnancyDashboard(current.profile, viewModel, onEdit = { editing = true })
        }
    }
}

@Composable
private fun CenterMessage(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeContentPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
    }
}

expect fun todayIsoDate(): String
