package com.jecsdev.gestiora.data.datasources.core.di

import com.jecsdev.gestiora.core.common.di.KoinModuleLoader
import org.koin.core.module.Module

object DataSourceCoreDependencyInjector : KoinModuleLoader {
    override val modules: List<Module> = listOf(secureProfileStoreModule(), secureAppointmentsStoreModule(), securePrenatalNotesStoreModule(), securePersonalRemindersStoreModule(), secureConsultationsStoreModule(), secureVitalRecordsStoreModule(), secureMedicalConditionsStoreModule(), secureVaccineRecordsStoreModule(), secureExamRecordsStoreModule(), themePreferenceModule())
}

internal expect fun secureProfileStoreModule(): Module
internal expect fun secureAppointmentsStoreModule(): Module
internal expect fun securePrenatalNotesStoreModule(): Module

internal expect fun secureConsultationsStoreModule(): Module

internal expect fun secureVitalRecordsStoreModule(): Module

internal expect fun secureMedicalConditionsStoreModule(): Module

internal expect fun secureVaccineRecordsStoreModule(): Module

internal expect fun secureExamRecordsStoreModule(): Module

internal expect fun themePreferenceModule(): Module

internal expect fun securePersonalRemindersStoreModule(): Module
