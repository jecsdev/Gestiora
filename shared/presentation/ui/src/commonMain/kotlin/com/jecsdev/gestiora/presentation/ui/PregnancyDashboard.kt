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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.presentation.viewmodels.ProfileViewModel

@Composable
fun PregnancyDashboard(profile: PregnancyProfile, viewModel: ProfileViewModel, onEdit: () -> Unit) {
    val age = viewModel.preview(profile.dating, todayIsoDate())
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
        Text("Información orientativa. Gestiora no diagnostica; consulta siempre a tu equipo de salud.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Tus citas y registros aparecerán aquí cuando estén disponibles.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
