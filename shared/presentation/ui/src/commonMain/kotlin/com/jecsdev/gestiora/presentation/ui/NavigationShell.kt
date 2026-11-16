package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.retain.retain
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

internal enum class AppDestination(val label: String) {
    Home("Inicio"), Calendar("Calendario"), Records("Registros"), Baby("Mi bebé"), Profile("Perfil")
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
    var navigationLocked by retain { mutableStateOf(false) }
    var calendarDate by retain { mutableStateOf<String?>(null) }
    var notesStatus by retain { mutableStateOf<NoteTaskStatus?>(null) }
    var openConditions by retain { mutableStateOf(false) }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    AppDestination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            enabled = !navigationLocked,
                            onClick = { calendarDate = null; notesStatus = null; openConditions = false; onDestinationChange(item) },
                            icon = { DestinationIcon(item) },
                            label = { Text(item.label) },
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
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
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
                AppDestination.Baby -> BabyScreen(profile, viewModel)
                AppDestination.Profile -> ProfileScreen(profile, onEdit, preferences)
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
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        EditorialHeader("Mi perfil", eyebrow = "TU ESPACIO PERSONAL")
        EditorialSection("Sobre ti y tu bebé") {
            EditorialRecordCard {
                ProfileLine("Tu nombre", profile.motherName ?: "No indicado")
                ProfileLine("Apodo del bebé", profile.babyName ?: "No indicado")
                ProfileLine("País", profile.country)
                ProfileLine("Idioma", profile.language)
            }
        }
        EditorialSection("Fechas del embarazo") {
            EditorialRecordCard {
                val method = when (profile.dating.method) {
                    DatingMethod.FUM -> "Última menstruación"
                    DatingMethod.ULTRASOUND -> "Ecografía"
                    DatingMethod.MEDICAL -> "Ajuste médico"
                }
                ProfileLine("Método de cálculo", method)
                profile.dating.lmp?.let { ProfileLine("Última menstruación", dateLabel(it)) }
                profile.dating.dueDate?.let { ProfileLine("Fecha probable de parto", dateLabel(it)) }
                profile.dating.referenceDate?.let { ProfileLine("Fecha de referencia", dateLabel(it)) }
                profile.dating.referenceWeeks?.let { weeks ->
                    ProfileLine("Edad en la referencia", "$weeks semanas y ${profile.dating.referenceDays ?: 0} días")
                }
            }
        }
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Editar mi perfil y fechas") }
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
