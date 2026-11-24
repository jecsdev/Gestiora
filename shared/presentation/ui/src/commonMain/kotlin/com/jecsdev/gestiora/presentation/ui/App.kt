package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import kotlin.coroutines.cancellation.CancellationException

class BackupControls(
    val canRestore: suspend () -> Boolean,
    val restored: Boolean,
    val restoreAction: @Composable () -> Unit,
    val exportAction: @Composable () -> Unit,
)

@Composable
fun App(
    platformModules: List<Module> = emptyList(),
    applicationGraph: org.koin.core.KoinApplication? = null,
    notificationControls: NotificationControls = NotificationControls(),
    backupControls: BackupControls? = null,
    nativeAd: NativeAdContent? = null,
    prenatalHistoryPreview: Boolean = false,
    premiumActive: Boolean = false,
    clinicalAttachment: ClinicalAttachmentContent? = null,
    prenatalReport: PrenatalReportControls? = null,
    pregnancyJournal: PregnancyJournalControls? = null,
) {
    val application = applicationGraph ?: retain { RetainedAppGraph(CoreDependencyInjection.modules + platformModules) }.application
    KoinIsolatedContext(context = application) {
        // Material controls also use rememberSaveable internally. Medical drafts stay in RAM.
        CompositionLocalProvider(
            LocalSaveableStateRegistry provides null,
            LocalNotificationControls provides notificationControls,
            LocalNativeAd provides nativeAd.takeUnless { premiumActive },
            LocalClinicalAttachment provides clinicalAttachment,
            LocalPrenatalReport provides prenatalReport,
            LocalPregnancyJournal provides pregnancyJournal,
        ) {
            UiCopyProvider {
                MotionPreferences {
                    ThemeRoot(koinInject()) { preferences ->
                        ProfileRoot(koinInject(), preferences, backupControls, prenatalHistoryPreview || premiumActive)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRoot(
    viewModel: ProfileViewModel,
    preferences: @Composable () -> Unit,
    backup: BackupControls?,
    prenatalHistoryPreview: Boolean,
) {
    val ui = LocalUiCopy.current
    var loadAttempt by retain { mutableIntStateOf(0) }
    var state by retain { mutableStateOf<ProfileLoad>(ProfileLoad.Loading) }
    var editing by retain { mutableStateOf(false) }
    var requestEditCancel by retain { mutableStateOf(false) }
    var destination by retain { mutableStateOf(AppDestination.Home) }
    var saving by retain { mutableStateOf(false) }
    var saveFailed by retain { mutableStateOf(false) }
    var canRestore by retain { mutableStateOf(false) }

    val operations = retainedOperationScope()
    LaunchedEffect(loadAttempt) {
        // Recreating a Ready/Empty root must not remove the retained form subtree.
        if (state == ProfileLoad.Loading) state = viewModel.load()
    }
    LaunchedEffect(state is ProfileLoad.Empty, backup != null) {
        canRestore = if (state == ProfileLoad.Empty && backup != null) {
            try {
                backup.canRestore()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
        } else false
    }
    LaunchedEffect(backup?.restored) {
        if (backup?.restored == true && state == ProfileLoad.Empty) {
            state = ProfileLoad.Loading
            state = viewModel.load()
        }
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
            GradientButton(onClick = { state = ProfileLoad.Loading; loadAttempt++ }) { Text(ui.s(Res.string.retry)) }
        }
        ProfileLoad.Empty -> OnboardingScreen(
            initialProfile = null,
            viewModel = viewModel,
            saving = saving,
            saveFailed = saveFailed,
            onSave = ::save,
            restoreBackup = backup?.restoreAction?.takeIf { canRestore },
            supportsBackup = backup != null,
        )
        is ProfileLoad.Ready -> {
            NavigationShell(current.profile, viewModel, destination, onDestinationChange = { destination = it },
                onEdit = { editing = true }, preferences = preferences, exportBackup = backup?.exportAction,
                prenatalHistoryPreview = prenatalHistoryPreview)
            if (editing) {
                RecordFormSheet(busy = saving, onDismissRequest = { requestEditCancel = true }) {
                    OnboardingScreen(
                        initialProfile = current.profile,
                        viewModel = viewModel,
                        saving = saving,
                        saveFailed = saveFailed,
                        onSave = ::save,
                        onCancel = { editing = false; saveFailed = false; requestEditCancel = false },
                        cancelRequested = requestEditCancel,
                        onCancelRequestHandled = { requestEditCancel = false },
                        supportsBackup = backup != null,
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterMessage(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
    }
}

expect fun todayIsoDate(): String
