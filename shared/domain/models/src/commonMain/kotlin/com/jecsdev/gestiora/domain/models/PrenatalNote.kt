package com.jecsdev.gestiora.domain.models

enum class NoteCategory { MEDICAL_INSTRUCTIONS, DOCTOR_QUESTIONS, MEDICATIONS, NUTRITION, SYMPTOMS, GENERAL }
enum class NoteTaskStatus { NONE, PENDING, COMPLETED }

data class PrenatalNote(
    val id: String,
    val title: String,
    val body: String? = null,
    val category: NoteCategory = NoteCategory.GENERAL,
    val status: NoteTaskStatus = NoteTaskStatus.NONE,
    val followUp: String? = null,
)
