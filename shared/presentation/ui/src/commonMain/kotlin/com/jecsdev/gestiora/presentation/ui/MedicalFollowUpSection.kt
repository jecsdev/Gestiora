package com.jecsdev.gestiora.presentation.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jecsdev.gestiora.domain.models.ConditionStatus
import com.jecsdev.gestiora.domain.models.MedicalCondition
import com.jecsdev.gestiora.presentation.viewmodels.MedicalConditionViewModel

/** Matches the prototype's recorded order; this does not rank medical urgency. */
internal fun firstMedicalFollowUp(conditions: List<MedicalCondition>): MedicalCondition? =
    conditions.firstOrNull { it.status != ConditionStatus.RESOLVED }

@Composable
internal fun MedicalFollowUpSection(viewModel: MedicalConditionViewModel, onOpen: () -> Unit) {
    var conditions by remember(viewModel) { mutableStateOf<List<MedicalCondition>?>(null) }
    var failed by remember(viewModel) { mutableStateOf(false) }
    var attempt by remember(viewModel) { mutableStateOf(0) }
    LaunchedEffect(viewModel, attempt) {
        failed = false
        conditions = null
        val result = viewModel.load()
        conditions = result.getOrNull()
        failed = result.isFailure
    }
    when {
        failed -> {
            EditorialSection("Seguimiento médico") {
                Text("No pudimos leer tus condiciones registradas. Tus datos no se han borrado.", color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { attempt++ }) { Text("Reintentar seguimiento médico") }
            }
        }
        conditions == null -> CircularProgressIndicator()
        else -> firstMedicalFollowUp(conditions!!)?.let { condition ->
            EditorialSection("Seguimiento médico") {
                EditorialFollowUpRow(condition.name, condition.instructions ?: "Revisa las indicaciones registradas.", onOpen)
            }
        }
    }
}
