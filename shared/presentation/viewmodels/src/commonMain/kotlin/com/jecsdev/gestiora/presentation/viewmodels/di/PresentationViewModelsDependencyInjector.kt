package com.jecsdev.gestiora.presentation.viewmodels.di

import com.jecsdev.gestiora.core.common.di.KoinModuleLoader
import com.jecsdev.gestiora.presentation.viewmodels.ProfileViewModel
import com.jecsdev.gestiora.presentation.viewmodels.PregnancyDatingViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

object PresentationViewModelsDependencyInjector : KoinModuleLoader {
    override val modules: List<Module> = listOf(module {
        factory { PregnancyDatingViewModel(get()) }
        single { ProfileViewModel(get(), get()) }
    })
}
