package com.jecsdev.gestiora.domain.usecases.di

import com.jecsdev.gestiora.core.common.di.KoinModuleLoader
import com.jecsdev.gestiora.domain.usecases.CalculatePregnancyAge
import org.koin.core.module.Module
import org.koin.dsl.module

object DomainUseCasesDependencyInjector : KoinModuleLoader {
    override val modules: List<Module> = listOf(module { single { CalculatePregnancyAge() } })
}
