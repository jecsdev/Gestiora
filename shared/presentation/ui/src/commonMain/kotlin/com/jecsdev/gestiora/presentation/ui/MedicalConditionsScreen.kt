package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.*
import com.jecsdev.gestiora.presentation.viewmodels.MedicalConditionViewModel
import kotlinx.coroutines.launch

@Composable
internal fun MedicalConditionsScreen(viewModel: MedicalConditionViewModel, onNavigationLockChange: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var conditions by remember { mutableStateOf<List<MedicalCondition>?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var operationError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MedicalCondition?>(null) }
    var deleting by remember { mutableStateOf<MedicalCondition?>(null) }
    var discard by remember { mutableStateOf(false) }
    suspend fun reload() {
        val result = viewModel.load()
        conditions = result.getOrNull()
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
        MedicalConditionForm(editing!!, busy, operationError, onCancel = { discard = true }) { condition ->
            busy = true
            scope.launch {
                try {
                    operationError = false
                    val result = if (condition.id.isEmpty()) viewModel.add(condition) else viewModel.update(condition)
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
        Text("Condiciones y riesgos", style = MaterialTheme.typography.headlineSmall)
        Text("Solo condiciones y riesgos indicados por un profesional de salud. Gestiora no identifica ni diagnostica riesgos.")
        when {
            loadError -> {
                Text("No pudimos leer tus condiciones. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(enabled = !busy, onClick = {
                    busy = true
                    onNavigationLockChange(true)
                    scope.launch { try { reload() } finally { busy = false; onNavigationLockChange(false) } }
                }) { Text("Reintentar") }
            }
            conditions == null -> CircularProgressIndicator()
            else -> {
                Button(enabled = !busy, onClick = { operationError = false; editing = MedicalCondition("", "", todayIsoDate(), ""); onNavigationLockChange(true) }) { Text("Nueva condición") }
                if (conditions!!.isEmpty()) Text("Aún no tienes condiciones registradas.")
                conditions!!.forEach { condition ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(condition.status.label(), style = MaterialTheme.typography.labelLarge)
                            Text(condition.name, style = MaterialTheme.typography.titleMedium)
                            Text(condition.professional)
                            Text(condition.date, style = MaterialTheme.typography.labelMedium)
                            condition.instructions?.let { MedicalConditionDetail("Indicaciones médicas", it) }
                            condition.treatment?.let { MedicalConditionDetail("Tratamiento prescrito", it) }
                            condition.monitoring?.let { MedicalConditionDetail("Requisitos de control", it) }
                            condition.followUp?.let { MedicalConditionDetail("Seguimiento", it) }
                            condition.notes?.let { MedicalConditionDetail("Notas históricas", it) }
                            Row {
                                TextButton(enabled = !busy, onClick = { operationError = false; editing = condition; onNavigationLockChange(true) }) { Text("Editar") }
                                TextButton(enabled = !busy, onClick = { deleting = condition; onNavigationLockChange(true) }) { Text("Eliminar") }
                            }
                        }
                    }
                }
                if (operationError) Text("No pudimos eliminar la condición. Inténtalo de nuevo.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    deleting?.let { condition ->
        AlertDialog(
            onDismissRequest = { if (!busy) { deleting = null; onNavigationLockChange(false) } },
            title = { Text("¿Eliminar condición?") },
            text = { Text("Se eliminará esta condición de este dispositivo.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        operationError = false
                        val result = viewModel.delete(condition.id)
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
private fun MedicalConditionDetail(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value)
}


@Composable
private fun MedicalConditionForm(initial: MedicalCondition, busy: Boolean, saveError: Boolean, onCancel: () -> Unit, onSave: (MedicalCondition) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var date by remember(initial) { mutableStateOf(initial.date) }
    var professional by remember(initial) { mutableStateOf(initial.professional) }
    var instructions by remember(initial) { mutableStateOf(initial.instructions.orEmpty()) }
    var treatment by remember(initial) { mutableStateOf(initial.treatment.orEmpty()) }
    var monitoring by remember(initial) { mutableStateOf(initial.monitoring.orEmpty()) }
    var followUp by remember(initial) { mutableStateOf(initial.followUp.orEmpty()) }
    var status by remember(initial) { mutableStateOf(initial.status) }
    var notes by remember(initial) { mutableStateOf(initial.notes.orEmpty()) }
    var invalid by remember(initial) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeContentPadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (initial.id.isEmpty()) "Nueva condición" else "Editar condición", style = MaterialTheme.typography.headlineLarge)
        Text("Solo condiciones y riesgos indicados por un profesional de salud. Gestiora no identifica ni diagnostica riesgos.", style = MaterialTheme.typography.bodySmall)
        MedicalConditionTextField("Condición (indicada por un profesional)", name, 512, busy, invalid && name.isBlank()) { name = it }
        DateField("Fecha identificada", date, enabled = !busy) { date = it }
        MedicalConditionTextField("Profesional de salud", professional, 512, busy, invalid && professional.isBlank()) { professional = it }
        MedicalConditionTextField("Indicaciones médicas (opcional)", instructions, 4096, busy) { instructions = it }
        MedicalConditionTextField("Tratamiento prescrito (opcional)", treatment, 4096, busy) { treatment = it }
        MedicalConditionTextField("Requisitos de control (opcional)", monitoring, 4096, busy) { monitoring = it }
        DateField("Fecha de seguimiento (opcional)", followUp, clearable = true, enabled = !busy) { followUp = it }
        Text("Esta fecha no crea una cita ni una notificación.", style = MaterialTheme.typography.bodySmall)
        Text("Estado", style = MaterialTheme.typography.labelLarge)
        ConditionStatus.entries.forEach { option ->
            FilterChip(status == option, { status = option }, enabled = !busy, label = { Text(option.label()) })
        }
        MedicalConditionTextField("Notas históricas (opcional)", notes, 4096, busy) { notes = it }
        if (invalid) Text("Indica la condición, el profesional y una fecha válida. Respeta los límites de los campos.", color = MaterialTheme.colorScheme.error)
        if (saveError) Text("No pudimos guardar la condición. Tus cambios siguen en el formulario.", color = MaterialTheme.colorScheme.error)
        Button(enabled = !busy, onClick = {
            val condition = medicalConditionForSave(initial, name, date, professional, instructions, treatment, monitoring, followUp, status, notes)
            invalid = condition == null
            if (condition != null) onSave(condition)
        }) { Text(if (busy) "Guardando…" else "Guardar condición") }
        OutlinedButton(enabled = !busy, onClick = onCancel) { Text("Cancelar") }
    }
}

@Composable
private fun MedicalConditionTextField(label: String, value: String, limit: Int, busy: Boolean, requiredError: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, enabled = !busy, singleLine = limit == 512, isError = requiredError || value.length > limit, supportingText = { Text("${value.length}/$limit") }, modifier = Modifier.fillMaxWidth())
}
