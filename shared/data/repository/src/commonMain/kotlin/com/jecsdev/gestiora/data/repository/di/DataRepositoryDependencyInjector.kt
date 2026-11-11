package com.jecsdev.gestiora.data.repository.di

import com.jecsdev.gestiora.core.common.di.KoinModuleLoader
import com.jecsdev.gestiora.data.repository.SecurePregnancyProfileRepository
import com.jecsdev.gestiora.domain.repository.PregnancyProfileRepository
import org.koin.core.module.Module
import org.koin.dsl.module

object DataRepositoryDependencyInjector : KoinModuleLoader {
    override val modules: List<Module> = listOf(module {
        single<PregnancyProfileRepository> { SecurePregnancyProfileRepository(get()) }
    })
}
