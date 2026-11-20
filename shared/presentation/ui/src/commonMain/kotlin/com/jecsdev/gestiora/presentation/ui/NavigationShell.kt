package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*
import org.jetbrains.compose.resources.StringResource

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.DatingMethod
import com.jecsdev.gestiora.domain.models.NoteTaskStatus
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.presentation.viewmodels.ProfileViewModel
import com.jecsdev.gestiora.presentation.viewmodels.AppointmentViewModel
import com.jecsdev.gestiora.presentation.viewmodels.MedicalConditionViewModel
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalNoteViewModel
import org.koin.compose.koinInject

internal enum class AppDestination(val label: StringResource) {
    Home(Res.string.home), Calendar(Res.string.calendar), Records(Res.string.records), Baby(Res.string.my_baby), Profile(Res.string.profile)
}

@Composable
internal fun NavigationShell(
    profile: PregnancyProfile,
    viewModel: ProfileViewModel,
    destination: AppDestination,
    onDestinationChange: (AppDestination) -> Unit,
    onEdit: () -> Unit,
    preferences: @Composable () -> Unit,
) {
    val ui = LocalUiCopy.current
    var navigationLocked by retain { mutableStateOf(false) }
    var calendarDate by retain { mutableStateOf<String?>(null) }
    var notesStatus by retain { mutableStateOf<NoteTaskStatus?>(null) }
    var openConditions by retain { mutableStateOf(false) }
    var catalogOpen by retain { mutableStateOf(false) }
    val catalogTransition = updateTransition(catalogOpen, label = "Learning library")
    val showBabyContent = !catalogTransition.currentState && !catalogTransition.targetState && !catalogTransition.isRunning
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (destination == AppDestination.Baby && !showBabyContent) return@Scaffold
            Surface(modifier = Modifier.padding(horizontal = 10.dp).navigationBarsPadding().padding(bottom = 6.dp),
                color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(30.dp), shadowElevation = 3.dp) {
                NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp, windowInsets = WindowInsets(0, 0, 0, 0)) {
                    AppDestination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            enabled = !navigationLocked,
                            onClick = { calendarDate = null; notesStatus = null; openConditions = false; onDestinationChange(item) },
                            icon = { DestinationIcon(item) },
                            label = { Text(ui.s(item.label), fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        },
    ) { padding ->
        key(destination) {
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).screenEntrance(12.dp)) {
                when (destination) {
                    AppDestination.Home -> PregnancyDashboard(profile, viewModel, koinInject<AppointmentViewModel>(), onProfile = { onDestinationChange(AppDestination.Profile) }, onBaby = { onDestinationChange(AppDestination.Baby) }, followUpContent = {
                        PendingNotesSection(koinInject<PrenatalNoteViewModel>(), todayIsoDate()) {
                            openConditions = false
                            notesStatus = NoteTaskStatus.PENDING
                            onDestinationChange(AppDestination.Records)
                        }
                        MedicalFollowUpSection(koinInject<MedicalConditionViewModel>()) {
                            notesStatus = null
                            openConditions = true
                            onDestinationChange(AppDestination.Records)
                        }
                    }) { date ->
                        calendarDate = date
                        onDestinationChange(AppDestination.Calendar)
                    }
                    AppDestination.Calendar -> CalendarScreen(calendarDate) { navigationLocked = it }
                    AppDestination.Records -> RecordsScreen(notesStatus, openConditions) { navigationLocked = it }
                    AppDestination.Baby -> BabyScreen(profile, viewModel, catalogTransition, showBabyContent) { catalogOpen = it }
                    AppDestination.Profile -> ProfileScreen(profile, onEdit, preferences)
                }
            }
    }
    }
}

@Composable
private fun UnavailableScreen(title: String, message: String) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ProfileScreen(profile: PregnancyProfile, onEdit: () -> Unit, preferences: @Composable () -> Unit) {
    val ui = LocalUiCopy.current
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        EditorialHeader(ui.s(Res.string.my_profile), eyebrow = ui.s(Res.string.your_personal_space))
        PastelHero {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(
                            profile.motherName?.firstOrNull()?.uppercase() ?: "G",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(profile.motherName?.takeIf { it.isNotBlank() } ?: ui.s(Res.string.my_profile), style = MaterialTheme.typography.titleLarge)
                    Text(countryLabel(profile.country, ui), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        EditorialSection(ui.s(Res.string.about_you_and_your_baby)) {
            EditorialRecordCard {
                ProfileLine(ui.s(Res.string.your_name), profile.motherName ?: ui.s(Res.string.not_provided))
                ProfileLine(ui.s(Res.string.baby_s_nickname), profile.babyName ?: ui.s(Res.string.not_provided))
                ProfileLine(ui.s(Res.string.country), countryLabel(profile.country, ui))
                ProfileLine(ui.s(Res.string.language), languageLabel(profile.language, ui))
            }
        }
        EditorialSection(ui.s(Res.string.pregnancy_dates)) {
            EditorialRecordCard {
                val method = when (profile.dating.method) {
                    DatingMethod.FUM -> ui.s(Res.string.last_menstrual_period)
                    DatingMethod.ULTRASOUND -> ui.s(Res.string.ultrasound)
                    DatingMethod.MEDICAL -> ui.s(Res.string.medical_adjustment)
                }
                ProfileLine(ui.s(Res.string.dating_method), method)
                profile.dating.lmp?.let { ProfileLine(ui.s(Res.string.last_menstrual_period), dateLabel(ui, it)) }
                profile.dating.dueDate?.let { ProfileLine(ui.s(Res.string.estimated_due_date), dateLabel(ui, it)) }
                profile.dating.referenceDate?.let { ProfileLine(ui.s(Res.string.reference_date), dateLabel(ui, it)) }
                profile.dating.referenceWeeks?.let { weeks ->
                    ProfileLine(ui.s(Res.string.age_at_reference_date), ui.s(Res.string.weeks_and_days, weeks, profile.dating.referenceDays ?: 0))
                }
            }
        }
        GradientButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text(ui.s(Res.string.edit_my_profile_and_dates)) }
        preferences()
    }
}

@Composable
private fun ProfileLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Simple local line icons; the navigation item's label supplies the accessible name. */
@Composable
internal fun DestinationIcon(destination: AppDestination) {
    val color = androidx.compose.material3.LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val s = size.width / 24f
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), 1.75f * s, cap = StrokeCap.Round)
        fun outline(vararg points: Pair<Float, Float>) {
            val path = Path().apply {
                moveTo(points[0].first * s, points[0].second * s)
                points.drop(1).forEach { (x, y) -> lineTo(x * s, y * s) }
            }
            drawPath(path, color, style = Stroke(1.75f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        when (destination) {
            AppDestination.Home -> outline(3f to 11f, 12f to 3f, 21f to 11f, 19f to 11f, 19f to 20f, 5f to 20f, 5f to 11f)
            AppDestination.Calendar -> {
                outline(4f to 5f, 20f to 5f, 20f to 20f, 4f to 20f, 4f to 5f)
                line(4f, 10f, 20f, 10f); line(8f, 3f, 8f, 7f); line(16f, 3f, 16f, 7f)
            }
            AppDestination.Records -> {
                outline(6f to 3f, 18f to 3f, 18f to 21f, 6f to 21f, 6f to 3f)
                line(9f, 9f, 15f, 9f); line(9f, 13f, 15f, 13f); line(9f, 17f, 15f, 17f)
            }
            AppDestination.Baby -> {
                drawCircle(color, 8f * s, Offset(12f * s, 12f * s), style = Stroke(1.75f * s))
                drawCircle(color, 1f * s, Offset(9f * s, 11f * s)); drawCircle(color, 1f * s, Offset(15f * s, 11f * s))
                line(10f, 15f, 14f, 15f)
            }
            AppDestination.Profile -> {
                drawCircle(color, 3.5f * s, Offset(12f * s, 7f * s), style = Stroke(1.75f * s))
                outline(5f to 21f, 5f to 19f, 7f to 16f, 10f to 15f, 14f to 15f, 17f to 16f, 19f to 19f, 19f to 21f)
            }
        }
    }
}
