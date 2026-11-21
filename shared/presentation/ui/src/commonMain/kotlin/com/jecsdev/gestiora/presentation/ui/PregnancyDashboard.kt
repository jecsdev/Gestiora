package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.domain.models.PregnancyAge
import com.jecsdev.gestiora.domain.models.Appointment
import com.jecsdev.gestiora.domain.models.AppointmentStatus
import com.jecsdev.gestiora.presentation.viewmodels.AppointmentViewModel
import com.jecsdev.gestiora.presentation.viewmodels.ProfileViewModel

internal fun upcomingAppointments(appointments: List<Appointment>, today: String): List<Appointment> =
    appointments.asSequence()
        .filter { it.status == AppointmentStatus.SCHEDULED && it.date >= today }
        .sortedWith(compareBy(Appointment::date, Appointment::time, Appointment::id))
        .take(3)
        .toList()

@Composable
fun PregnancyDashboard(
    profile: PregnancyProfile,
    viewModel: ProfileViewModel,
    appointmentViewModel: AppointmentViewModel,
    onProfile: () -> Unit,
    onBaby: () -> Unit,
    onSettings: () -> Unit,
    followUpContent: @Composable () -> Unit = {},
    onCalendar: (String?) -> Unit,
) {
    val today = todayIsoDate()
    val age = viewModel.preview(profile.dating, today)
    var appointments by remember(appointmentViewModel) { mutableStateOf<Result<List<Appointment>>?>(null) }
    var loadAttempt by remember(appointmentViewModel) { mutableStateOf(0) }
    LaunchedEffect(appointmentViewModel, loadAttempt) {
        appointments = null
        appointments = appointmentViewModel.load().map { upcomingAppointments(it, today) }
    }
    PregnancyDashboardContent(
        motherName = profile.motherName,
        age = age,
        appointments = appointments,
        onRetry = { loadAttempt++ },
        onProfile = onProfile,
        onBaby = onBaby,
        onSettings = onSettings,
        followUpContent = followUpContent,
        onCalendar = onCalendar,
    )
}

@Composable
private fun PregnancyDashboardContent(
    motherName: String?,
    age: PregnancyAge?,
    appointments: Result<List<Appointment>>?,
    onRetry: () -> Unit,
    onProfile: () -> Unit,
    onBaby: () -> Unit,
    onSettings: () -> Unit,
    followUpContent: @Composable () -> Unit,
    onCalendar: (String?) -> Unit,
) {
    val ui = LocalUiCopy.current
    Column(
        Modifier.fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(ui.s(Res.string.brand), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    IconButton(onClick = onProfile, modifier = Modifier.semantics { contentDescription = ui.s(Res.string.open_my_profile) }) {
                        DestinationIcon(AppDestination.Profile)
                    }
                }
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    IconButton(onClick = onSettings, modifier = Modifier.semantics { contentDescription = ui.s(Res.string.settings_title) }) {
                        SettingsIcon()
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (motherName.isNullOrBlank()) ui.s(Res.string.hello) else ui.s(Res.string.hello_2, motherName), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(ui.s(Res.string.your_pregnancy), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        }
        PastelHero {
                if (age == null) {
                    Text(ui.s(Res.string.the_saved_dates_are_no_longer_within_the), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = onProfile) { Text(ui.s(Res.string.review_dates_in_my_profile)) }
                } else {
                    Text(ui.s(Res.string.trimester_current_week, age.trimester), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    PregnancyWeekIllustration(age)
                    LinearProgressIndicator(progress = { age.progress.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text(ui.s(Res.string.your_journey, (age.progress * 100).toInt()), style = MaterialTheme.typography.labelMedium)
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), shape = RoundedCornerShape(24.dp)) {
                        Box(Modifier.padding(16.dp)) { PregnancyDateSummary(age) }
                    }
                }
        }
        EditorialSection(ui.s(Res.string.upcoming_appointments), ui.s(Res.string.view_calendar), onAction = { onCalendar(null) }) {
            val upcoming = appointments?.getOrNull().orEmpty()
            when {
                appointments?.isFailure == true -> {
                    Text(ui.s(Res.string.we_couldn_t_read_your_appointments_your_data), color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry) { Text(ui.s(Res.string.retry)) }
                }
                appointments == null -> CircularProgressIndicator()
                upcoming.isEmpty() -> {
                    EditorialRecordCard {
                        Text(ui.s(Res.string.no_upcoming_appointments_add_your_next_prenatal_check), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> upcoming.forEach { appointment ->
                    AppointmentPreview(appointment, onClick = { onCalendar(appointment.date) })
                }
            }
        }
        if (age != null) {
            EditorialSection(if (age.weeks in 4..40) ui.s(Res.string.this_week) else ui.s(Res.string.explore_development), ui.s(Res.string.view_baby), onAction = onBaby) {
                EditorialCard(onClick = onBaby) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (age.weeks !in 4..40) Text(ui.s(Res.string.your_current_week_is_educational_reference_for_week, age.weeks, age.weeks.coerceIn(4, 40)), style = MaterialTheme.typography.bodySmall)
                        val stage = babyStage(age.weeks)
                        Text(ui.s(Res.string.about_the_size_of_2, ui.s(stage.size)), style = MaterialTheme.typography.titleMedium)
                        Text(ui.s(stage.text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        followUpContent()
        HomeQuestions()
        Text(ui.s(Res.string.educational_information_gestiora_does_not_diagnose_always_consult), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LocalNativeAd.current?.invoke(NativeAdPlacement.HomeFooter)
    }
}

@Preview(name = "Home · upcoming appointment", widthDp = 384, heightDp = 850, locale = "en")
@Composable
private fun PregnancyDashboardPreview() {
    PreviewContent {
        PregnancyDashboardContent(
            motherName = previewProfile.motherName,
            age = PregnancyAge(112, 16, 0, 2, "2027-04-03", 168, 0.4),
            appointments = Result.success(listOf(
                Appointment(
                    id = "preview-appointment",
                    date = "2026-10-22",
                    time = "09:30",
                    doctor = "Dr. Rivera",
                    specialty = "Obstetrics",
                    clinic = "Example Clinic",
                    reason = "Prenatal checkup",
                    status = AppointmentStatus.SCHEDULED,
                ),
            )),
            onRetry = {},
            onProfile = {},
            onBaby = {},
            onSettings = {},
            followUpContent = {},
            onCalendar = {},
        )
    }
}

@Composable
private fun AppointmentPreview(appointment: Appointment, onClick: () -> Unit) {
    val ui = LocalUiCopy.current
    val fontScale = LocalDensity.current.fontScale
    EditorialCard(onClick) {
        BoxWithConstraints(Modifier.padding(16.dp)) {
            val date: @Composable () -> Unit = {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                    Column(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            .clearAndSetSemantics { contentDescription = dateLabel(ui, appointment.date, expanded = true) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(appointment.date.takeLast(2).toInt().toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text(dateLabel(ui, appointment.date).substringAfter(' '), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            val details: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(appointment.reason, style = MaterialTheme.typography.titleMedium)
                    Text(appointment.time, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(appointment.doctor, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (maxWidth < 280.dp || fontScale > 1.2f) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { date(); details() }
            } else Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                date()
                Column(Modifier.weight(1f)) { details() }
            }
        }
    }
}

@Composable
private fun PregnancyWeekIllustration(age: PregnancyAge) {
    val ui = LocalUiCopy.current
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 270.dp || fontScale > 1.2f
        val summary: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${age.weeks}", style = MaterialTheme.typography.displayLarge.copy(fontSize = 76.sp), fontWeight = FontWeight.Medium)
                Text(ui.s(Res.string.weeks_days, age.days), style = MaterialTheme.typography.bodyLarge)
            }
        }
        val illustration: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(if (stacked) 160.dp else 124.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f), CircleShape), contentAlignment = Alignment.Center) {
                    BabyIllustration(age.weeks, Modifier.size(if (stacked) 152.dp else 116.dp))
                }
                Text(ui.s(Res.string.reference_week, babyReferenceWeek(age.weeks)), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (stacked) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            summary()
            illustration()
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { summary() }
            illustration()
        }
    }
}

@Composable
private fun PregnancyDateSummary(age: PregnancyAge) {
    val ui = LocalUiCopy.current
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val due: @Composable () -> Unit = {
            Column {
                Text(ui.s(Res.string.estimated_due_date_3), style = MaterialTheme.typography.labelSmall)
                Text(dateLabel(ui, age.dueDate), fontWeight = FontWeight.Bold)
            }
        }
        val remaining: @Composable () -> Unit = {
            Column {
                Text(ui.s(Res.string.until_we_meet), style = MaterialTheme.typography.labelSmall)
                Text(ui.s(Res.string.days_count, age.daysLeft.coerceAtLeast(0)), fontWeight = FontWeight.Bold)
            }
        }
        if (maxWidth < 240.dp || fontScale > 1.2f) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            due()
            remaining()
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) { due() }
            Column(Modifier.weight(1f)) { remaining() }
        }
    }
}
