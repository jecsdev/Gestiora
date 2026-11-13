package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.DatingMethod
import com.jecsdev.gestiora.domain.models.PregnancyDating
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.presentation.viewmodels.ProfileViewModel
import gestiora.shared.presentation.ui.generated.resources.Res
import gestiora.shared.presentation.ui.generated.resources.fetus_week_36
import org.jetbrains.compose.resources.painterResource

private val countries = listOf("Argentina", "Bolivia", "Chile", "Colombia", "Ecuador", "España", "Estados Unidos", "México", "Paraguay", "Perú", "Uruguay", "Venezuela", "Otro")

@Composable
fun OnboardingScreen(
    initialProfile: PregnancyProfile?,
    viewModel: ProfileViewModel,
    saving: Boolean,
    saveFailed: Boolean,
    onSave: (PregnancyProfile) -> Unit,
    onCancel: () -> Unit = {},
) {
    var step by retain(initialProfile) { mutableIntStateOf(if (initialProfile == null) 0 else 1) }
    var motherName by retain(initialProfile) { mutableStateOf(initialProfile?.motherName.orEmpty()) }
    var babyName by retain(initialProfile) { mutableStateOf(initialProfile?.babyName.orEmpty()) }
    var country by retain(initialProfile) { mutableStateOf(initialProfile?.country.orEmpty()) }
    var method by retain(initialProfile) { mutableStateOf(initialProfile?.dating?.method ?: DatingMethod.FUM) }
    var lmp by retain(initialProfile) { mutableStateOf(initialProfile?.dating?.lmp.orEmpty()) }
    var dueDate by retain(initialProfile) { mutableStateOf(initialProfile?.dating?.dueDate.orEmpty()) }
    var referenceDate by retain(initialProfile) { mutableStateOf(initialProfile?.dating?.referenceDate ?: todayIsoDate()) }
    var referenceWeeks by retain(initialProfile) { mutableStateOf(initialProfile?.dating?.referenceWeeks?.toString().orEmpty()) }
    var referenceDays by retain(initialProfile) { mutableStateOf(initialProfile?.dating?.referenceDays?.toString() ?: "0") }
    var error by retain { mutableStateOf("") }
    var discardOpen by retain(initialProfile) { mutableStateOf(false) }

    fun back() {
        when (onboardingBackAction(step, initialProfile != null, saving, discardOpen)) {
            OnboardingBackAction.BLOCK, OnboardingBackAction.SYSTEM -> Unit
            OnboardingBackAction.DISMISS_DISCARD -> discardOpen = false
            OnboardingBackAction.PREVIOUS_STEP -> { step--; error = "" }
            OnboardingBackAction.CONFIRM_DISCARD -> discardOpen = true
        }
    }
    AppointmentBackHandler(saving || step > 0 || initialProfile != null || discardOpen, ::back)
    if (discardOpen) AlertDialog(
        onDismissRequest = { if (!saving) discardOpen = false },
        title = { Text("¿Descartar cambios del perfil?") },
        text = { Text("Los cambios que no hayas guardado se perderán.") },
        confirmButton = { TextButton(enabled = !saving, onClick = { discardOpen = false; onCancel() }) { Text("Descartar") } },
        dismissButton = { TextButton(enabled = !saving, onClick = { discardOpen = false }) { Text("Seguir editando") } },
    )

    val dating = PregnancyDating(
        method = method,
        lmp = lmp.takeIf { method == DatingMethod.FUM && it.isNotBlank() },
        dueDate = dueDate.takeIf { method == DatingMethod.FUM && it.isNotBlank() },
        referenceDate = referenceDate.takeIf { method != DatingMethod.FUM && it.isNotBlank() },
        referenceWeeks = referenceWeeks.toIntOrNull().takeIf { method != DatingMethod.FUM },
        referenceDays = referenceDays.toIntOrNull().takeIf { method != DatingMethod.FUM },
    )
    val weeks = referenceWeeks.toIntOrNull()
    val days = referenceDays.toIntOrNull()
    val validReference = method == DatingMethod.FUM ||
        (referenceDate.isNotBlank() && referenceDate <= todayIsoDate() && weeks != null && weeks in 0..42 && days != null && days in 0..6)
    val age = if (validReference) viewModel.preview(dating, todayIsoDate()) else null

    val datingToSave = profileDatingToSave(dating, initialProfile?.dating, validReference && age != null)

    fun next() {
        error = when (step) {
            1 -> if (country.isBlank()) "Selecciona tu país." else ""
            2 -> if (datingToSave != null) "" else when {
                method == DatingMethod.FUM && lmp.isBlank() && dueDate.isBlank() -> "Indica la fecha de tu última menstruación o la fecha probable de parto."
                !validReference -> "Indica la fecha y una edad gestacional válida (0–42 semanas, 0–6 días)."
                age == null -> "Las fechas no corresponden a un embarazo en curso. Revísalas."
                else -> ""
            }
            else -> ""
        }
        if (error.isEmpty()) step++
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeContentPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("GESTIORA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        if (step == 0) {
            Box(Modifier.fillMaxWidth().height(260.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(32.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(Res.drawable.fetus_week_36), contentDescription = "Ilustración fetal de referencia", modifier = Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
            }
            Text("TU ESPACIO PRENATAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Tu embarazo, en calma.", style = MaterialTheme.typography.displaySmall)
            Text("Citas, registros y cada pequeño avance, reunidos en un lugar íntimo y sencillo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Comenzar") }
            Text("Puedes comenzar sin crear una cuenta.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = ::back, enabled = !saving) { Text("Atrás") }
                Column(Modifier.weight(1f)) {
                    Text("TU CONFIGURACIÓN · $step DE 3", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { step / 3f }, modifier = Modifier.fillMaxWidth())
                }
            }
            when (step) {
                1 -> {
                    Text("Cuéntanos un poco de ti", style = MaterialTheme.typography.headlineLarge)
                    Text("Los nombres son opcionales; puedes omitirlos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(motherName, { motherName = it }, label = { Text("Tu nombre (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(babyName, { babyName = it }, label = { Text("Apodo del bebé (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    CountryField(country) { country = it }
                    Text("Idioma preferido: Español", style = MaterialTheme.typography.bodyMedium)
                    Text("Más idiomas llegarán en próximas versiones.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                2 -> {
                    Text("Fechas del embarazo", style = MaterialTheme.typography.headlineLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            DatingMethod.FUM to "Última menstruación",
                            DatingMethod.ULTRASOUND to "Ecografía",
                            DatingMethod.MEDICAL to "Ajuste médico",
                        ).forEach { (value, label) ->
                            FilterChip(selected = method == value, onClick = { method = value; error = "" }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (method == DatingMethod.FUM) {
                        DateField("Primer día de la última menstruación", lmp, maxDate = todayIsoDate(), clearable = true) { lmp = it }
                        DateField("Fecha probable de parto (si la conoces)", dueDate, clearable = true) { dueDate = it }
                    } else {
                        DateField("Fecha en que se estableció", referenceDate, maxDate = todayIsoDate()) { referenceDate = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(referenceWeeks, { referenceWeeks = it }, label = { Text("Semanas") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(referenceDays, { referenceDays = it }, label = { Text("Días") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                    }
                    if (age != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Hoy: ${age.weeks} sem ${age.days} d", fontWeight = FontWeight.Bold)
                                Text("Fecha probable de parto: ${age.dueDate}")
                            }
                        }
                    }
                    if (age == null && datingToSave != null && initialProfile != null) Text("Tus fechas guardadas se conservarán. Puedes actualizar tus datos personales sin cambiarlas.", style = MaterialTheme.typography.bodySmall)
                    Text("Los cálculos son estimaciones. La fecha de ecografía o ajuste médico tiene prioridad.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                3 -> {
                    Text("Tu información es tuya", style = MaterialTheme.typography.headlineLarge)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Este perfil se guardará protegido en el dispositivo. Aún no hay cuenta ni sincronización Cloud.")
                            Text("Gestiora no diagnostica ni reemplaza a tu equipo de salud.")
                            Text("La recuperación de datos en otro dispositivo todavía no está disponible.")
                        }
                    }
                }
            }
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (saveFailed) Text("No pudimos guardar tu perfil. Reintenta; tus datos del formulario siguen aquí.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = {
                    if (step < 3) next()
                    else {
                        // The confirmation step can remain open across a calendar-day change.
                        val submitToday = todayIsoDate()
                        val submittedDating = profileDatingToSave(dating, initialProfile?.dating,
                            validReference && (method == DatingMethod.FUM || referenceDate <= submitToday) &&
                                viewModel.preview(dating, submitToday) != null)
                        if (submittedDating == null) { step = 2; error = "Revisa las fechas del embarazo antes de guardar." }
                        else onSave(PregnancyProfile(motherName.trim().ifEmpty { null }, babyName.trim().ifEmpty { null }, country, "Español", submittedDating))
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (saving) "Guardando…" else if (step < 3) "Continuar" else "Empezar a usar Gestiora") }
        }
    }
}
@Composable
private fun CountryField(value: String, onValueChange: (String) -> Unit) {
    var expanded by retain { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(if (value.isEmpty()) "País * · Selecciona…" else "País * · $value") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            countries.forEach { country -> DropdownMenuItem(text = { Text(country) }, onClick = { onValueChange(country); expanded = false }) }
        }
    }
}
