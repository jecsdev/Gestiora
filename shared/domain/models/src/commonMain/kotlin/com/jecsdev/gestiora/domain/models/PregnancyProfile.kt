package com.jecsdev.gestiora.domain.models

/** Device-local pregnancy profile. Historical medical records are stored separately. */
data class PregnancyProfile(
    val motherName: String? = null,
    val babyName: String? = null,
    val country: String,
    val language: String = "Español",
    val dating: PregnancyDating,
)
