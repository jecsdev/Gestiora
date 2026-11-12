package com.jecsdev.gestiora.domain.models

enum class ExamKind { ULTRASOUND, BLOOD_TEST, URINE_TEST, GLUCOSE, OTHER }
enum class ExamStatus { PENDING, SCHEDULED, COMPLETED }

/** A recorded study and its supplied results; no medical interpretation is generated. */
data class ExamRecord(
    val id: String,
    val name: String,
    val kind: ExamKind = ExamKind.BLOOD_TEST,
    val requested: String? = null,
    val scheduled: String? = null,
    val completed: String? = null,
    val status: ExamStatus = ExamStatus.PENDING,
    val physician: String? = null,
    val results: String? = null,
    val notes: String? = null,
    val attachment: String? = null,
)
