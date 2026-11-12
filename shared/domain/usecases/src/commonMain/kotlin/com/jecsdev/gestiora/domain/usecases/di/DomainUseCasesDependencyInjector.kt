package com.jecsdev.gestiora.domain.usecases.di

import com.jecsdev.gestiora.core.common.di.KoinModuleLoader
import com.jecsdev.gestiora.domain.usecases.CalculatePregnancyAge
import com.jecsdev.gestiora.domain.usecases.ManageAppointments
import org.koin.core.module.Module
import org.koin.dsl.module
import com.jecsdev.gestiora.domain.usecases.ManagePrenatalNotes

import com.jecsdev.gestiora.domain.usecases.ManageConsultations

import com.jecsdev.gestiora.domain.usecases.ManageVitalRecords

import com.jecsdev.gestiora.domain.usecases.ManageMedicalConditions

import com.jecsdev.gestiora.domain.usecases.ManageVaccineRecords

import com.jecsdev.gestiora.domain.usecases.ManageExamRecords

object DomainUseCasesDependencyInjector : KoinModuleLoader {
    override val modules: List<Module> = listOf(module {
        single { CalculatePregnancyAge() }
        single { ManageAppointments(get()) }
        single { ManagePrenatalNotes(get()) }
        single { ManageConsultations(get()) }
        single { ManageVitalRecords(get()) }
        single { ManageMedicalConditions(get()) }
        single { ManageVaccineRecords(get()) }
        single { ManageExamRecords(get()) }
    })
}
