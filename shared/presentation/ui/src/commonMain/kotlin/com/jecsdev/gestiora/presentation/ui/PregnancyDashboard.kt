package com.jecsdev.gestiora.presentation.ui

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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
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
fun PregnancyDashboard(profile: PregnancyProfile, viewModel: ProfileViewModel, appointmentViewModel: AppointmentViewModel, onProfile: () -> Unit, onBaby: () -> Unit, followUpContent: @Composable () -> Unit = {}, onCalendar: (String?) -> Unit) {
    val today = todayIsoDate()
    val age = viewModel.preview(profile.dating, today)
    var appointments by remember(appointmentViewModel) { mutableStateOf<List<Appointment>?>(null) }
    var loadError by remember(appointmentViewModel) { mutableStateOf(false) }
    var loadAttempt by remember(appointmentViewModel) { mutableStateOf(0) }
    LaunchedEffect(appointmentViewModel, loadAttempt) {
        loadError = false
        appointments = null
        val result = appointmentViewModel.load()
        appointments = result.getOrNull()?.let { upcomingAppointments(it, today) }
        loadError = result.isFailure
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("GESTIORA", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                IconButton(onClick = onProfile, modifier = Modifier.semantics { contentDescription = "Abrir mi perfil" }) {
                    DestinationIcon(AppDestination.Profile)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (profile.motherName.isNullOrBlank()) "Hola" else "Hola, ${profile.motherName}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Tu embarazo", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (age == null) {
                    Text("Las fechas guardadas ya no corresponden al rango de un embarazo en curso.", style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = onProfile) { Text("Revisar fechas en mi perfil") }
                } else {
                    Text("TRIMESTRE ${age.trimester} · SEMANA ACTUAL", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    PregnancyWeekIllustration(age)
                    LinearProgressIndicator(progress = { age.progress.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text("Tu recorrido · ${(age.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (age != null) PregnancyDateSummary(age)
        EditorialSection("Próximas citas", "Ver calendario", onAction = { onCalendar(null) }) {
            when {
                loadError -> {
                    Text("No pudimos leer tus citas. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { loadAttempt++ }) { Text("Reintentar") }
                }
                appointments == null -> CircularProgressIndicator()
                appointments!!.isEmpty() -> {
                    Text("Sin citas próximas. Agrega tu próximo control prenatal.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> appointments!!.forEach { appointment ->
                    AppointmentPreview(appointment, onClick = { onCalendar(appointment.date) })
                }
            }
        }
        if (age != null) {
            EditorialSection(if (age.weeks in 4..40) "Esta semana" else "Explora su desarrollo", "Ver bebé", onAction = onBaby) {
                EditorialCard(onClick = onBaby) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (age.weeks !in 4..40) Text("Tu semana actual es ${age.weeks}. Referencia educativa de la semana ${age.weeks.coerceIn(4, 40)}; el explorador cubre las semanas 4 a 40.", style = MaterialTheme.typography.bodySmall)
                        val stage = babyStage(age.weeks)
                        Text("Tamaño aproximado de ${stage.size}", style = MaterialTheme.typography.titleMedium)
                        Text(stage.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        followUpContent()
        Text("Información orientativa. Gestiora no diagnostica; consulta siempre a tu equipo de salud.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppointmentPreview(appointment: Appointment, onClick: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    EditorialCard(onClick) {
        BoxWithConstraints(Modifier.padding(16.dp)) {
            val date: @Composable () -> Unit = {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                    Column(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            .clearAndSetSemantics { contentDescription = dateLabel(appointment.date, expanded = true) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(appointment.date.takeLast(2).toInt().toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text(dateLabel(appointment.date).substringAfter(' '), style = MaterialTheme.typography.labelSmall)
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
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 320.dp || fontScale > 1.2f
        val summary: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${age.weeks}", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Medium)
                Text("semanas · ${age.days} días", style = MaterialTheme.typography.bodyLarge)
            }
        }
        val illustration: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BabyIllustration(age.weeks, Modifier.size(if (stacked) 180.dp else 144.dp))
                Text("Referencia: semana ${babyReferenceWeek(age.weeks)}", style = MaterialTheme.typography.labelSmall)
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
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val due: @Composable () -> Unit = {
            Column {
                Text("FECHA PROBABLE", style = MaterialTheme.typography.labelSmall)
                Text(dateLabel(age.dueDate), fontWeight = FontWeight.Bold)
            }
        }
        val remaining: @Composable () -> Unit = {
            Column {
                Text("POR CONOCERTE", style = MaterialTheme.typography.labelSmall)
                Text("${age.daysLeft.coerceAtLeast(0)} días", fontWeight = FontWeight.Bold)
            }
        }
        if (maxWidth < 320.dp || fontScale > 1.2f) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            due()
            remaining()
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            due()
            remaining()
        }
    }
}
