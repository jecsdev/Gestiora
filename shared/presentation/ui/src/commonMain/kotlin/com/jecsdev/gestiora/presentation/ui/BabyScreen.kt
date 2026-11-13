package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.presentation.viewmodels.ProfileViewModel
import kotlin.math.roundToInt

internal data class BabyStage(val from: Int, val size: String, val text: String, val milestones: List<String>)

private val stages = listOf(
    BabyStage(0, "una semilla de amapola", "Se forman las estructuras iniciales y comienza el desarrollo del tubo neural.", listOf("Implantación", "Inicio del tubo neural", "Se forma el corazón primitivo")),
    BabyStage(8, "una frambuesa", "Se desarrollan dedos, rasgos faciales y órganos principales.", listOf("Dedos en desarrollo", "Movimientos espontáneos", "Órganos en formación")),
    BabyStage(12, "una lima", "Las estructuras principales continúan desarrollándose.", listOf("Reflejos iniciales", "Uñas en desarrollo", "Fin del primer trimestre")),
    BabyStage(16, "un aguacate", "Los músculos se fortalecen y puede reaccionar a estímulos.", listOf("Movimientos coordinados", "Esqueleto en endurecimiento", "Oído en desarrollo")),
    BabyStage(20, "un plátano", "Muchas personas empiezan a notar movimientos con mayor claridad.", listOf("Ciclos de sueño", "Vérnix protector", "Movimientos perceptibles")),
    BabyStage(24, "una mazorca de maíz", "Los pulmones y el cerebro siguen madurando rápidamente.", listOf("Responde a sonidos", "Pulmones en desarrollo", "Gana grasa corporal")),
    BabyStage(28, "una berenjena", "Puede abrir y cerrar los ojos y aumenta el peso.", listOf("Abre los ojos", "Crecimiento cerebral", "Inicio del tercer trimestre")),
    BabyStage(32, "una calabaza pequeña", "Acumula grasa y practica movimientos respiratorios.", listOf("Uñas completas", "Huesos más firmes", "Posición cambiante")),
    BabyStage(36, "una lechuga romana", "Continúa ganando peso y preparándose para el nacimiento.", listOf("Maduración pulmonar", "Menos espacio para moverse", "Preparación final")),
)

internal fun babyStage(week: Int): BabyStage = stages.last { week.coerceIn(4, 40) >= it.from }
internal fun babyReferenceWeek(week: Int): Int = listOf(36, 28, 20, 16, 12, 8, 4).first { week.coerceIn(4, 40) >= it }

@Composable
fun BabyScreen(profile: PregnancyProfile, viewModel: ProfileViewModel) {
    val actualWeek = viewModel.preview(profile.dating, todayIsoDate())?.weeks
    val nearestWeek = actualWeek?.coerceIn(4, 40)
    var chosen by retain(profile.dating) { mutableStateOf<Int?>(null) }
    val week = chosen ?: nearestWeek ?: 4
    val stage = babyStage(week)
    val referenceWeek = babyReferenceWeek(week)
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeContentPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Cada semana, un pequeño milagro", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(profile.babyName?.takeIf { it.isNotBlank() } ?: "Mi bebé", style = MaterialTheme.typography.headlineLarge)
        if (actualWeek == null) Text("Revisa las fechas de tu perfil para conocer tu semana actual.", style = MaterialTheme.typography.bodySmall)
        if (actualWeek != null && actualWeek !in 4..40) Text("Tu semana actual es $actualWeek. El explorador muestra las semanas 4 a 40.", style = MaterialTheme.typography.bodySmall)
        Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Explora su desarrollo", style = MaterialTheme.typography.labelMedium)
                Text("Semana $week", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { chosen = week - 1 }, enabled = week > 4, modifier = Modifier.semantics { contentDescription = "Semana anterior" }) { Text("‹") }
                    OutlinedButton(onClick = { chosen = week + 1 }, enabled = week < 40, modifier = Modifier.semantics { contentDescription = "Semana siguiente" }) { Text("›") }
                }
                BabyIllustration(week, Modifier.fillMaxWidth().heightIn(max = 280.dp))
                Text("Etapa ilustrativa de referencia: semana $referenceWeek · No representa medidas exactas.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                Slider(value = week.toFloat(), onValueChange = { chosen = it.roundToInt().coerceIn(4, 40) }, valueRange = 4f..40f, steps = 35, modifier = Modifier.semantics { contentDescription = "Semana del embarazo" })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Semana 4", style = MaterialTheme.typography.labelSmall)
                    Text("Semana 40", style = MaterialTheme.typography.labelSmall)
                }
                if (nearestWeek != null && week != nearestWeek) TextButton(onClick = { chosen = null }) {
                    Text(if (actualWeek == nearestWeek) "Volver a mi semana ($actualWeek)" else "Volver a la semana $nearestWeek")
                }
            }
        }
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Desarrollo aproximado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Como ${stage.size}", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            }
        }
        Text("Pequeños grandes avances", style = MaterialTheme.typography.headlineSmall)
        Text(stage.text, style = MaterialTheme.typography.bodyMedium)
        stage.milestones.forEachIndexed { index, milestone ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("0${index + 1}", color = MaterialTheme.colorScheme.primary)
                    Text(milestone, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Text("Cada embarazo es diferente. Esta información es orientativa, no una evaluación clínica.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
