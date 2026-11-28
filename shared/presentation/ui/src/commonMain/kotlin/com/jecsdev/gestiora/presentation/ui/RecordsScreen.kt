package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*
import org.jetbrains.compose.resources.StringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ScrollState
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
internal fun RecordsScreen(
    initialNotesStatus: NoteTaskStatus?,
    selected: RecordSection,
    sectionsScroll: ScrollState,
    onSectionChange: (RecordSection) -> Unit,
    onPremiumPlans: () -> Unit,
    onHistory: (() -> Unit)?,
    onNavigationLockChange: (Boolean) -> Unit,
) {
    val ui = LocalUiCopy.current
    var locked by retain { mutableStateOf(false) }
    val lock: (Boolean) -> Unit = { locked = it; onNavigationLockChange(it) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
        if (!locked) {
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)) {
                EditorialHeader(ui.s(Res.string.my_records), eyebrow = ui.s(Res.string.your_prenatal_history))
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(sectionsScroll).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecordSection.entries.forEach { section ->
                FilterChip(selected == section, { onSectionChange(section) }, enabled = !locked, label = { Text(ui.s(section.label)) })
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Box(Modifier.weight(1f)) {
            when (selected) {
                RecordSection.HEALTH -> VitalRecordsScreen(koinInject<VitalRecordViewModel>(), lock)
                RecordSection.CONSULTATIONS -> ConsultationsScreen(koinInject<ConsultationViewModel>(), lock)
                RecordSection.CONDITIONS -> MedicalConditionsScreen(koinInject<MedicalConditionViewModel>(), lock)
                RecordSection.VACCINES -> VaccineRecordsScreen(koinInject<VaccineRecordViewModel>(), lock)
                RecordSection.EXAMS -> ExamRecordsScreen(
                    viewModel = koinInject<ExamRecordViewModel>(),
                    onNavigationLockChange = lock,
                    onPremiumPlans = onPremiumPlans,
                    onHistory = onHistory,
                )
                RecordSection.NOTES -> PrenatalNotesScreen(koinInject<PrenatalNoteViewModel>(), initialNotesStatus, lock)
            }
        }
    }
}

internal enum class RecordSection(val label: StringResource) {
    HEALTH(Res.string.health), CONSULTATIONS(Res.string.consultations), CONDITIONS(Res.string.conditions), VACCINES(Res.string.vaccines), EXAMS(Res.string.examinations), NOTES(Res.string.notes_and_tasks)
}
