package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.*
import com.jecsdev.gestiora.presentation.viewmodels.ConsultationViewModel
import kotlinx.coroutines.launch

@Composable
internal fun ConsultationsScreen(viewModel: ConsultationViewModel, onNavigationLockChange: (Boolean) -> Unit) {
    val scope = retainedOperationScope()
    var consultations by retain { mutableStateOf<List<Consultation>?>(null) }
    var loadError by retain { mutableStateOf(false) }
    var operationError by retain { mutableStateOf(false) }
    var busy by retain { mutableStateOf(false) }
    var editing by retain { mutableStateOf<Consultation?>(null) }
    var deleting by retain { mutableStateOf<Consultation?>(null) }
    var discard by retain { mutableStateOf(false) }
    suspend fun reload() {
        val result = viewModel.load()
        consultations = result.getOrNull()
        loadError = result.isFailure
    }
    LaunchedEffect(viewModel) { reload() }
    AppointmentBackHandler(busy || editing != null || deleting != null) {
        when {
            busy -> Unit
            discard -> discard = false
            deleting != null -> { deleting = null; onNavigationLockChange(false) }
            editing != null -> discard = true
        }
    }
    if (discard) AlertDialog(
        onDismissRequest = { discard = false },
        title = { Text("¿Descartar cambios?") },
        text = { Text("Los cambios que no hayas guardado se perderán.") },
        confirmButton = { TextButton(onClick = { discard = false; editing = null; operationError = false; onNavigationLockChange(false) }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Seguir editando") } },
    )
    if (editing != null) {
        ConsultationForm(editing!!, busy, operationError, onCancel = { discard = true }) { consultation ->
            busy = true
            scope.launch {
                try {
                    operationError = false
                    val result = if (consultation.id.isEmpty()) viewModel.add(consultation) else viewModel.update(consultation)
                    if (result.isSuccess) {
                        editing = null
                        reload()
                    } else operationError = true
                } finally { busy = false; if (editing == null) onNavigationLockChange(false) }
            }
        }
        return
    }
    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Consultas prenatales", style = MaterialTheme.typography.headlineSmall)
        Text("Tu historial se guarda en este dispositivo. La edad gestacional registrada se conserva aunque cambies las fechas del embarazo.")
        when {
            loadError -> {
                Text("No pudimos leer tus consultas. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text("Reintentar") }
            }
            consultations == null -> CircularProgressIndicator()
            else -> {
                Button(enabled = !busy, onClick = { operationError = false; editing = Consultation("", todayIsoDate()); onNavigationLockChange(true) }) { Text("Nueva consulta") }
                if (consultations!!.isEmpty()) Text("Aún no tienes consultas registradas.")
                consultations!!.sortedWith(compareByDescending<Consultation> { it.date }.thenBy { it.id }).forEach { consultation ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(consultation.doctor ?: "Consulta prenatal", style = MaterialTheme.typography.titleMedium)
                            Text(consultation.date, style = MaterialTheme.typography.labelLarge)
                            consultation.gestationalAge?.let { ConsultationDetail("Edad gestacional registrada", it) }
                            consultation.observations?.let { ConsultationDetail("Observaciones", it) }
                            consultation.instructions?.let { ConsultationDetail("Indicaciones", it) }
                            consultation.medications?.let { ConsultationDetail("Medicamentos", it) }
                            consultation.exams?.let { ConsultationDetail("Estudios", it) }
                            consultation.followUp?.let { ConsultationDetail("Seguimiento", it) }
                            consultation.nextDate?.let { ConsultationDetail("Próxima cita", it) }
                            Row {
                                TextButton(enabled = !busy, onClick = { operationError = false; editing = consultation; onNavigationLockChange(true) }) { Text("Editar") }
                                TextButton(enabled = !busy, onClick = { deleting = consultation; onNavigationLockChange(true) }) { Text("Eliminar") }
                            }
                        }
                    }
                }
                if (operationError) Text("No pudimos eliminar la consulta. Inténtalo de nuevo.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { consultation ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text("¿Eliminar consulta?") },
            text = { Text("Se eliminará esta consulta de este dispositivo.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        operationError = false
                        val result = viewModel.delete(consultation.id)
                        deleting = null
                        if (result.isSuccess) reload() else operationError = true
                    } finally { busy = false; onNavigationLockChange(false) }
                }
            }) { Text("Eliminar") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null; onNavigationLockChange(false) }) { Text("Cancelar") } },
        )
    }
}


@Composable
private fun ConsultationDetail(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value)
}

@Composable
private fun ConsultationForm(initial: Consultation, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onSave: (Consultation) -> Unit) {
    var date by retain(initial) { mutableStateOf(initial.date) }
    var ga by retain(initial) { mutableStateOf(initial.gestationalAge.orEmpty()) }
    var doctor by retain(initial) { mutableStateOf(initial.doctor.orEmpty()) }
    var observations by retain(initial) { mutableStateOf(initial.observations.orEmpty()) }
    var instructions by retain(initial) { mutableStateOf(initial.instructions.orEmpty()) }
    var medications by retain(initial) { mutableStateOf(initial.medications.orEmpty()) }
    var exams by retain(initial) { mutableStateOf(initial.exams.orEmpty()) }
    var followUp by retain(initial) { mutableStateOf(initial.followUp.orEmpty()) }
    var nextDate by retain(initial) { mutableStateOf(initial.nextDate.orEmpty()) }
    var invalid by retain(initial) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (initial.id.isEmpty()) "Nueva consulta" else "Editar consulta", style = MaterialTheme.typography.headlineLarge)
        DateField("Fecha de consulta", date, enabled = !busy) { date = it }
        ConsultationTextField("Edad gestacional registrada (opcional)", ga, 512, busy) { ga = it }
        Text("Copia la edad gestacional indicada en esa consulta, por ejemplo: 24 sem 2 d. Este dato no se recalcula.", style = MaterialTheme.typography.bodySmall)
        ConsultationTextField("Médico/a (opcional)", doctor, 512, busy) { doctor = it }
        ConsultationTextField("Observaciones del médico (opcional)", observations, 4096, busy) { observations = it }
        ConsultationTextField("Indicaciones médicas (opcional)", instructions, 4096, busy) { instructions = it }
        ConsultationTextField("Medicamentos recetados (opcional)", medications, 4096, busy) { medications = it }
        ConsultationTextField("Estudios solicitados (opcional)", exams, 4096, busy) { exams = it }
        ConsultationTextField("Requisitos de seguimiento (opcional)", followUp, 4096, busy) { followUp = it }
        DateField("Fecha de la próxima cita (opcional)", nextDate, clearable = true, enabled = !busy) { nextDate = it }
        Text("Esta fecha no crea una cita en el calendario ni una notificación.", style = MaterialTheme.typography.bodySmall)
        if (invalid) Text("Selecciona una fecha válida y respeta los límites de los campos.", color = MaterialTheme.colorScheme.error)
        if (saveError) Text("No pudimos guardar la consulta. Tus cambios siguen en el formulario.", color = MaterialTheme.colorScheme.error)
        Button(enabled = !busy, onClick = {
            invalid = isoToUtcMillis(date) == null || (nextDate.isNotEmpty() && isoToUtcMillis(nextDate) == null) || ga.length > 512 || doctor.length > 512 || listOf(observations, instructions, medications, exams, followUp).any { it.length > 4096 }
            fun optional(value: String) = value.trim().ifEmpty { null }
            if (!invalid) onSave(initial.copy(date = date, gestationalAge = optional(ga), doctor = optional(doctor), observations = optional(observations), instructions = optional(instructions), medications = optional(medications), exams = optional(exams), followUp = optional(followUp), nextDate = nextDate.ifEmpty { null }))
        }) { Text(if (busy) "Guardando…" else "Guardar consulta") }
        OutlinedButton(enabled = !busy, onClick = onCancel) { Text("Cancelar") }
    }
}

@Composable
private fun ConsultationTextField(label: String, value: String, limit: Int, busy: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, enabled = !busy, singleLine = limit == 512, isError = value.length > limit, supportingText = { Text("${value.length}/$limit") }, modifier = Modifier.fillMaxWidth())
}
