package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.PregnancyProfile
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
fun PregnancyDashboard(profile: PregnancyProfile, viewModel: ProfileViewModel, appointmentViewModel: AppointmentViewModel, onEdit: () -> Unit, followUpContent: @Composable () -> Unit = {}, onCalendar: (String?) -> Unit) {
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
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeContentPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("GESTIORA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        Text(if (profile.motherName.isNullOrBlank()) "Hola" else "Hola, ${profile.motherName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Tu embarazo", style = MaterialTheme.typography.headlineLarge)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(30.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (age == null) {
                    Text("Las fechas guardadas ya no corresponden al rango de un embarazo en curso.", style = MaterialTheme.typography.bodyLarge)
                    Text("Revisa las fechas de tu perfil.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("TRIMESTRE ${age.trimester} · SEMANA ACTUAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("${age.weeks} sem", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text("${age.days} días · ${age.weeks} sem ${age.days} d", style = MaterialTheme.typography.bodyMedium)
                    Text("Tu recorrido · ${(age.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    LinearProgressIndicator(progress = { age.progress.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("FECHA PROBABLE", style = MaterialTheme.typography.labelSmall)
                            Text(age.dueDate, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("POR CONOCERTE", style = MaterialTheme.typography.labelSmall)
                            Text("${age.daysLeft.coerceAtLeast(0)} días", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Editar mi perfil y fechas") }
        Text("Próximas citas", style = MaterialTheme.typography.titleLarge)
        when {
            loadError -> {
                Text("No pudimos leer tus citas. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { loadAttempt++ }) { Text("Reintentar") }
            }
            appointments == null -> CircularProgressIndicator()
            appointments!!.isEmpty() -> Text("Sin citas próximas. Agrega tu próximo control prenatal.")
            else -> appointments!!.forEach { appointment ->
                Card(onClick = { onCalendar(appointment.date) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(appointment.reason, style = MaterialTheme.typography.titleMedium)
                        Text("${appointment.date} · ${appointment.time} · ${appointment.doctor}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        OutlinedButton(onClick = { onCalendar(null) }, modifier = Modifier.fillMaxWidth()) { Text("Ver calendario") }
        followUpContent()
        Text("Información orientativa. Gestiora no diagnostica; consulta siempre a tu equipo de salud.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
