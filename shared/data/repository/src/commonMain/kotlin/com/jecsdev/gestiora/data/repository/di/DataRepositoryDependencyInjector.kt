package com.jecsdev.gestiora.data.repository.di

import com.jecsdev.gestiora.core.common.di.KoinModuleLoader
import com.jecsdev.gestiora.data.repository.SecurePregnancyProfileRepository
import com.jecsdev.gestiora.data.repository.SecureAppointmentRepository
import com.jecsdev.gestiora.domain.repository.AppointmentRepository
import com.jecsdev.gestiora.domain.repository.PregnancyProfileRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import com.jecsdev.gestiora.data.repository.SecurePrenatalNoteRepository
import com.jecsdev.gestiora.domain.repository.PrenatalNoteRepository

import com.jecsdev.gestiora.data.repository.SecureConsultationRepository
import com.jecsdev.gestiora.domain.repository.ConsultationRepository

import com.jecsdev.gestiora.data.repository.SecureVitalRecordRepository
import com.jecsdev.gestiora.domain.repository.VitalRecordRepository

import com.jecsdev.gestiora.data.repository.SecureMedicalConditionRepository
import com.jecsdev.gestiora.domain.repository.MedicalConditionRepository

import com.jecsdev.gestiora.data.repository.SecureVaccineRecordRepository
import com.jecsdev.gestiora.domain.repository.VaccineRecordRepository

import com.jecsdev.gestiora.data.repository.SecureExamRecordRepository
import com.jecsdev.gestiora.domain.repository.ExamRecordRepository

object DataRepositoryDependencyInjector : KoinModuleLoader {
    override val modules: List<Module> = listOf(module {
        single<PregnancyProfileRepository> { SecurePregnancyProfileRepository(get()) }
        single<AppointmentRepository> { SecureAppointmentRepository(get()) }
        single<PrenatalNoteRepository> { SecurePrenatalNoteRepository(get()) }
        single<ConsultationRepository> { SecureConsultationRepository(get()) }
        single<VitalRecordRepository> { SecureVitalRecordRepository(get()) }
        single<MedicalConditionRepository> { SecureMedicalConditionRepository(get()) }
        single<VaccineRecordRepository> { SecureVaccineRecordRepository(get()) }
        single<ExamRecordRepository> { SecureExamRecordRepository(get()) }
    })
}
