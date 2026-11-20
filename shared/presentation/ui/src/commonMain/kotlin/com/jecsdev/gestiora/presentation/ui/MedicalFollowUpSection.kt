package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

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
    val ui = LocalUiCopy.current
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
            EditorialSection(ui.s(Res.string.medical_follow_up)) {
                Text(ui.s(Res.string.we_couldn_t_read_your_recorded_conditions_your), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { attempt++ }) { Text(ui.s(Res.string.retry_medical_follow_up)) }
            }
        }
        conditions == null -> CircularProgressIndicator()
        else -> firstMedicalFollowUp(conditions!!)?.let { condition ->
            EditorialSection(ui.s(Res.string.medical_follow_up)) {
                EditorialFollowUpRow(condition.name, condition.instructions ?: ui.s(Res.string.review_the_recorded_instructions), onOpen)
            }
        }
    }
}
