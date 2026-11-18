package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.core.di.CoreDependencyInjection
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.presentation.viewmodels.ProfileLoad
import com.jecsdev.gestiora.presentation.viewmodels.ProfileViewModel
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.koinInject
import org.koin.core.module.Module
import kotlinx.coroutines.launch

@Composable
fun App(
    platformModules: List<Module> = emptyList(),
    applicationGraph: org.koin.core.KoinApplication? = null,
    notificationControls: NotificationControls = NotificationControls(),
) {
    val application = applicationGraph ?: retain { RetainedAppGraph(CoreDependencyInjection.modules + platformModules) }.application
    KoinIsolatedContext(context = application) {
        // Material controls also use rememberSaveable internally. Medical drafts stay in RAM.
        CompositionLocalProvider(LocalSaveableStateRegistry provides null, LocalNotificationControls provides notificationControls) {
            UiCopyProvider {
                MotionPreferences {
                    ThemeRoot(koinInject()) { preferences -> ProfileRoot(koinInject(), preferences) }
                }
            }
        }
    }
}

@Composable
private fun ProfileRoot(viewModel: ProfileViewModel, preferences: @Composable () -> Unit) {
    val ui = LocalUiCopy.current
    var loadAttempt by retain { mutableIntStateOf(0) }
    var state by retain { mutableStateOf<ProfileLoad>(ProfileLoad.Loading) }
    var editing by retain { mutableStateOf(false) }
    var destination by retain { mutableStateOf(AppDestination.Home) }
    var saving by retain { mutableStateOf(false) }
    var saveFailed by retain { mutableStateOf(false) }

    val operations = retainedOperationScope()
    LaunchedEffect(loadAttempt) {
        // Recreating a Ready/Empty root must not remove the retained form subtree.
        if (state == ProfileLoad.Loading) state = viewModel.load()
    }
    fun save(profile: PregnancyProfile) {
        if (saving) return
        saving = true
        saveFailed = false
        operations.launch {
            try {
                if (viewModel.save(profile)) {
                    state = ProfileLoad.Ready(profile)
                    editing = false
                } else saveFailed = true
            } finally { saving = false }
        }
    }

    when (val current = state) {
        ProfileLoad.Loading -> CenterMessage { CircularProgressIndicator() }
        ProfileLoad.Error -> CenterMessage {
            Text(ui.s(Res.string.we_couldn_t_read_your_profile_your_data), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { state = ProfileLoad.Loading; loadAttempt++ }) { Text(ui.s(Res.string.retry)) }
        }
        ProfileLoad.Empty -> OnboardingScreen(
            initialProfile = null,
            viewModel = viewModel,
            saving = saving,
            saveFailed = saveFailed,
            onSave = ::save,
        )
        is ProfileLoad.Ready -> if (editing) {
            OnboardingScreen(
                initialProfile = current.profile,
                viewModel = viewModel,
                saving = saving,
                saveFailed = saveFailed,
                onSave = ::save,
                onCancel = { editing = false; saveFailed = false },
            )
        } else {
            NavigationShell(current.profile, viewModel, destination, onDestinationChange = { destination = it }, onEdit = { editing = true }, preferences = preferences)
        }
    }
}

@Composable
private fun CenterMessage(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
    }
}

expect fun todayIsoDate(): String
