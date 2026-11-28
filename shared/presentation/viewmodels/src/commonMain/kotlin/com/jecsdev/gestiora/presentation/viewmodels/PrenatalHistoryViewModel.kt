package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.Consultation
import com.jecsdev.gestiora.domain.models.ExamRecord

sealed interface PrenatalHistoryEntry {
    val date: String?

    data class Visit(val consultation: Consultation) : PrenatalHistoryEntry {
        override val date: String get() = consultation.date
    }

    data class Study(val exam: ExamRecord) : PrenatalHistoryEntry {
        override val date: String? get() = exam.completed ?: exam.scheduled ?: exam.requested
    }
}

sealed interface PrenatalHistoryUiState {
    data object Loading : PrenatalHistoryUiState
    data object Unavailable : PrenatalHistoryUiState
    data class Ready(val entries: List<PrenatalHistoryEntry>) : PrenatalHistoryUiState
}

class PrenatalHistoryViewModel(
    private val consultations: ConsultationViewModel,
    private val exams: ExamRecordViewModel,
) {
    suspend fun load(): PrenatalHistoryUiState {
        val visits = consultations.load().getOrElse { return PrenatalHistoryUiState.Unavailable }
        val studies = exams.load().getOrElse { return PrenatalHistoryUiState.Unavailable }
        val timeline = buildList {
            visits.forEach { add(PrenatalHistoryEntry.Visit(it)) }
            studies.forEach { add(PrenatalHistoryEntry.Study(it)) }
        }
        return PrenatalHistoryUiState.Ready(timeline.sortedByDescending { it.date })
    }
}
