package com.jecsdev.gestiora.data.datasources.core.di

import com.jecsdev.gestiora.core.common.di.KoinModuleLoader
import org.koin.core.module.Module

object DataSourceCoreDependencyInjector : KoinModuleLoader {
    override val modules: List<Module> = listOf(secureProfileStoreModule(), secureAppointmentsStoreModule(), securePrenatalNotesStoreModule())
}

internal expect fun secureProfileStoreModule(): Module
internal expect fun secureAppointmentsStoreModule(): Module
internal expect fun securePrenatalNotesStoreModule(): Module
