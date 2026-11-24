package com.jecsdev.gestiora.domain.models

data class PregnancyJournalEntry(
    val id: String,
    val date: String,
    val title: String,
    val body: String,
) {
    companion object {
        const val MAX_TITLE = 120
        const val MAX_BODY = 4096
    }
}
