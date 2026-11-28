package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.Consultation
import com.jecsdev.gestiora.domain.models.ExamRecord
import com.jecsdev.gestiora.domain.models.ExamStatus
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalHistoryEntry
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalHistoryUiState
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalHistoryViewModel
import gestiora.shared.presentation.ui.generated.resources.*
import org.koin.compose.koinInject

@Composable
internal fun PrenatalHistoryRoute(
    onBack: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    onReport: (() -> Unit)? = null,
) {
    val viewModel = koinInject<PrenatalHistoryViewModel>()
    var loadAttempt by remember { mutableIntStateOf(0) }
    var openedEntry by retain { mutableStateOf<PrenatalHistoryEntry?>(null) }
    val state by produceState<PrenatalHistoryUiState>(PrenatalHistoryUiState.Loading, viewModel, loadAttempt) {
        value = PrenatalHistoryUiState.Loading
        value = viewModel.load()
    }
    ScreenExchange(openedEntry) { entry ->
        if (entry == null) {
            PrenatalHistoryScreen(
                state = state,
                onBack = onBack,
                onRetry = { loadAttempt++ },
                onOpenEntry = { openedEntry = it },
                listState = listState,
                onReport = onReport,
            )
        } else {
            val closeEntry = { openedEntry = null }
            AppointmentBackHandler(enabled = true, onBack = closeEntry)
            PrenatalHistoryDetail(entry, onBack = closeEntry)
        }
    }
}

@Composable
private fun PrenatalHistoryScreen(
    state: PrenatalHistoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenEntry: (PrenatalHistoryEntry) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    onReport: (() -> Unit)? = null,
) {
    val ui = LocalUiCopy.current
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenBackButton(onBack) }
        item {
            EditorialHeader(
                title = ui.s(Res.string.premium_history),
                supporting = ui.s(Res.string.premium_history_timeline_summary),
            )
        }
        when (state) {
            PrenatalHistoryUiState.Loading -> item { CircularProgressIndicator() }
            PrenatalHistoryUiState.Unavailable -> item {
                EditorialRecordCard {
                    Text(ui.s(Res.string.premium_history_error), color = MaterialTheme.colorScheme.error)
                    GradientButton(onClick = onRetry) { Text(ui.s(Res.string.retry)) }
                }
            }
            is PrenatalHistoryUiState.Ready -> {
                if (onReport != null) {
                    item {
                        GradientButton(onClick = onReport, modifier = Modifier.fillMaxWidth()) {
                            Text(ui.s(Res.string.premium_history_create_report))
                        }
                    }
                }
                if (state.entries.isEmpty()) {
                    item { EditorialEmptyState(ui.s(Res.string.premium_history_empty)) }
                }
                items(state.entries) { entry -> HistoryCard(entry, onOpen = { onOpenEntry(entry) }) }
            }
        }
    }
}

@Composable
private fun HistoryCard(entry: PrenatalHistoryEntry, onOpen: () -> Unit) {
    val ui = LocalUiCopy.current
    EditorialRecordCard {
        val date = entry.date?.let { dateLabel(ui, it) } ?: ui.s(Res.string.no_date_recorded)
        Text(date, style = MaterialTheme.typography.labelMedium)
        when (entry) {
            is PrenatalHistoryEntry.Visit -> {
                Text(ui.s(Res.string.premium_history_visit), style = MaterialTheme.typography.titleMedium)
                entry.consultation.doctor?.takeIf { it.isNotBlank() }?.let { Text(it) }
            }
            is PrenatalHistoryEntry.Study -> {
                Text(entry.exam.name, style = MaterialTheme.typography.titleMedium)
                EditorialStatus(entry.exam.status.label(ui))
            }
        }
        HistoryRecordSource()
        TextButton(onClick = onOpen) { Text(ui.s(Res.string.premium_history_view_record)) }
    }
}

@Composable
private fun PrenatalHistoryDetail(entry: PrenatalHistoryEntry, onBack: () -> Unit) {
    val ui = LocalUiCopy.current
    val title = when (entry) {
        is PrenatalHistoryEntry.Visit -> ui.s(Res.string.premium_history_visit)
        is PrenatalHistoryEntry.Study -> entry.exam.name
    }
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenBackButton(onBack)
        EditorialHeader(title, supporting = entry.date?.let { dateLabel(ui, it, expanded = true) })
        EditorialRecordCard {
            HistoryRecordSource()
            when (entry) {
                is PrenatalHistoryEntry.Visit -> {
                    entry.consultation.doctor?.takeIf { it.isNotBlank() }?.let { doctor ->
                        Text(ui.s(Res.string.doctor), style = MaterialTheme.typography.labelMedium)
                        Text(doctor)
                    }
                    ConsultationRecordDetails(entry.consultation)
                    ClinicalAttachmentActions(ClinicalAttachmentKind.Consultation, entry.consultation.id, true, showImages = true)
                }
                is PrenatalHistoryEntry.Study -> {
                    EditorialStatus(entry.exam.status.label(ui))
                    ExamRecordDetails(entry.exam)
                    ClinicalAttachmentActions(ClinicalAttachmentKind.Exam, entry.exam.id, true, showImages = true)
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordSource() {
    Text(
        LocalUiCopy.current.s(Res.string.premium_history_recorded_by_you),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "Prenatal history · light", widthDp = 360, heightDp = 780, locale = "es")
@Preview(name = "Prenatal history · dark", widthDp = 360, heightDp = 780, locale = "es", uiMode = 0x20)
@Composable
private fun PrenatalHistoryPreview() {
    PreviewContent(dark = isSystemInDarkTheme()) {
        PrenatalHistoryScreen(
            state = PrenatalHistoryUiState.Ready(
                listOf(
                    PrenatalHistoryEntry.Study(
                        ExamRecord("study", "Ecografía obstétrica", completed = "2026-11-15", status = ExamStatus.COMPLETED),
                    ),
                    PrenatalHistoryEntry.Visit(Consultation("visit", "2026-11-12")),
                ),
            ),
            onBack = {},
            onRetry = {},
            onOpenEntry = {},
        )
    }
}

@Preview(name = "Prenatal history · study detail", widthDp = 360, heightDp = 780, locale = "es")
@Composable
private fun PrenatalHistoryDetailPreview() {
    PreviewContent {
        PrenatalHistoryDetail(
            entry = PrenatalHistoryEntry.Study(
                ExamRecord(
                    id = "study",
                    name = "Ecografía obstétrica",
                    completed = "2026-11-15",
                    status = ExamStatus.COMPLETED,
                    attachment = "Informe de ejemplo.pdf",
                ),
            ),
            onBack = {},
        )
    }
}
