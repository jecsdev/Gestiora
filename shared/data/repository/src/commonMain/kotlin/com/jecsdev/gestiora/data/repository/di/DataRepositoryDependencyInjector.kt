package com.jecsdev.gestiora.data.repository.di

import com.jecsdev.gestiora.core.common.di.KoinModuleLoader
import com.jecsdev.gestiora.data.datasources.LegacyClinicalPayloadValidator
import com.jecsdev.gestiora.data.repository.LocalEducationalRepository
import com.jecsdev.gestiora.domain.repository.EducationalRepository
import com.jecsdev.gestiora.data.repository.ClinicalPayloadValidator
import com.jecsdev.gestiora.data.repository.SecurePregnancyProfileRepository
import com.jecsdev.gestiora.data.repository.SecureAppointmentRepository
import com.jecsdev.gestiora.domain.repository.AppointmentRepository
import com.jecsdev.gestiora.domain.repository.PregnancyProfileRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import com.jecsdev.gestiora.data.repository.SecurePrenatalNoteRepository
import com.jecsdev.gestiora.data.repository.SecurePersonalReminderRepository
import com.jecsdev.gestiora.domain.repository.PrenatalNoteRepository
import com.jecsdev.gestiora.domain.repository.PersonalReminderRepository

import com.jecsdev.gestiora.data.repository.SecureConsultationRepository
import com.jecsdev.gestiora.domain.repository.ConsultationRepository

import com.jecsdev.gestiora.data.repository.SecureVitalRecordRepository
import com.jecsdev.gestiora.domain.repository.VitalRecordRepository

import com.jecsdev.gestiora.data.repository.SecureMedicalConditionRepository
import com.jecsdev.gestiora.domain.repository.MedicalConditionRepository

import com.jecsdev.gestiora.data.repository.SecureVaccineRecordRepository
import com.jecsdev.gestiora.domain.repository.VaccineRecordRepository

import com.jecsdev.gestiora.data.repository.SecureExamRecordRepository
import com.jecsdev.gestiora.data.repository.LocalThemePreferenceRepository
import com.jecsdev.gestiora.domain.repository.ThemePreferenceRepository
import com.jecsdev.gestiora.domain.repository.ExamRecordRepository

object DataRepositoryDependencyInjector : KoinModuleLoader {
    override val modules: List<Module> = listOf(module {
        single<LegacyClinicalPayloadValidator> { ClinicalPayloadValidator() }
        single<EducationalRepository> { LocalEducationalRepository(get()) }
        single<PregnancyProfileRepository> { SecurePregnancyProfileRepository(get()) }
        single<AppointmentRepository> { SecureAppointmentRepository(get()) }
        single<PrenatalNoteRepository> { SecurePrenatalNoteRepository(get()) }
        single<PersonalReminderRepository> { SecurePersonalReminderRepository(get()) }
        single<ConsultationRepository> { SecureConsultationRepository(get()) }
        single<VitalRecordRepository> { SecureVitalRecordRepository(get()) }
        single<MedicalConditionRepository> { SecureMedicalConditionRepository(get()) }
        single<VaccineRecordRepository> { SecureVaccineRecordRepository(get()) }
        single<ExamRecordRepository> { SecureExamRecordRepository(get()) }
        single<ThemePreferenceRepository> { LocalThemePreferenceRepository(get()) }
    })
}
