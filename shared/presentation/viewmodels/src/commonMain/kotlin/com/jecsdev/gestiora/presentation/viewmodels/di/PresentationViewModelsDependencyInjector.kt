package com.jecsdev.gestiora.presentation.viewmodels.di

import com.jecsdev.gestiora.core.common.di.KoinModuleLoader
import com.jecsdev.gestiora.presentation.viewmodels.ProfileViewModel
import com.jecsdev.gestiora.presentation.viewmodels.AppointmentViewModel
import com.jecsdev.gestiora.presentation.viewmodels.PregnancyDatingViewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import com.jecsdev.gestiora.presentation.viewmodels.PrenatalNoteViewModel

import com.jecsdev.gestiora.presentation.viewmodels.ConsultationViewModel

import com.jecsdev.gestiora.presentation.viewmodels.VitalRecordViewModel

import com.jecsdev.gestiora.presentation.viewmodels.MedicalConditionViewModel

import com.jecsdev.gestiora.presentation.viewmodels.VaccineRecordViewModel

import com.jecsdev.gestiora.presentation.viewmodels.ExamRecordViewModel

object PresentationViewModelsDependencyInjector : KoinModuleLoader {
    override val modules: List<Module> = listOf(module {
        factory { PregnancyDatingViewModel(get()) }
        single { ProfileViewModel(get(), get()) }
        single { AppointmentViewModel(get()) }
        single { PrenatalNoteViewModel(get()) }
        single { ConsultationViewModel(get()) }
        single { VitalRecordViewModel(get()) }
        single { MedicalConditionViewModel(get()) }
        single { VaccineRecordViewModel(get()) }
        single { ExamRecordViewModel(get()) }
    })
}
