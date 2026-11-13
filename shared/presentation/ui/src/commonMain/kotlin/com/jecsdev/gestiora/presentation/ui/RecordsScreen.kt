package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.NoteTaskStatus
import com.jecsdev.gestiora.presentation.viewmodels.MedicalConditionViewModel
import com.jecsdev.gestiora.presentation.viewmodels.ConsultationViewModel
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalNoteViewModel
import com.jecsdev.gestiora.presentation.viewmodels.VitalRecordViewModel
import com.jecsdev.gestiora.presentation.viewmodels.VaccineRecordViewModel
import com.jecsdev.gestiora.presentation.viewmodels.ExamRecordViewModel
import org.koin.compose.koinInject

@Composable
internal fun RecordsScreen(initialNotesStatus: NoteTaskStatus?, initialConditions: Boolean = false, onNavigationLockChange: (Boolean) -> Unit) {
    var selected by retain { mutableStateOf(if (initialNotesStatus != null) RecordSection.NOTES else if (initialConditions) RecordSection.CONDITIONS else RecordSection.HEALTH) }
    var locked by retain { mutableStateOf(false) }
    val lock: (Boolean) -> Unit = { locked = it; onNavigationLockChange(it) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecordSection.entries.forEach { section ->
                FilterChip(selected == section, { selected = section }, enabled = !locked, label = { Text(section.label) })
            }
        }
        Box(Modifier.weight(1f)) {
            when (selected) {
                RecordSection.HEALTH -> VitalRecordsScreen(koinInject<VitalRecordViewModel>(), lock)
                RecordSection.CONSULTATIONS -> ConsultationsScreen(koinInject<ConsultationViewModel>(), lock)
                RecordSection.CONDITIONS -> MedicalConditionsScreen(koinInject<MedicalConditionViewModel>(), lock)
                RecordSection.VACCINES -> VaccineRecordsScreen(koinInject<VaccineRecordViewModel>(), lock)
                RecordSection.EXAMS -> ExamRecordsScreen(koinInject<ExamRecordViewModel>(), lock)
                RecordSection.NOTES -> PrenatalNotesScreen(koinInject<PrenatalNoteViewModel>(), initialNotesStatus, lock)
            }
        }
    }
}

private enum class RecordSection(val label: String) {
    HEALTH("Salud"), CONSULTATIONS("Consultas"), CONDITIONS("Condiciones"), VACCINES("Vacunas"), EXAMS("Estudios"), NOTES("Notas y tareas")
}
