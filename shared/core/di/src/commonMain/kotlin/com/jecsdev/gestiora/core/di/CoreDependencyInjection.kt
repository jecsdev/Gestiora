package com.jecsdev.gestiora.core.di

import com.jecsdev.gestiora.core.common.di.KoinModuleLoader
import com.jecsdev.gestiora.core.ui.di.CoreUiDependencyInjector
import com.jecsdev.gestiora.data.datasources.core.di.DataSourceCoreDependencyInjector
import com.jecsdev.gestiora.data.repository.di.DataRepositoryDependencyInjector
import com.jecsdev.gestiora.domain.usecases.di.DomainUseCasesDependencyInjector
import com.jecsdev.gestiora.presentation.viewmodels.di.PresentationViewModelsDependencyInjector
import org.koin.core.module.Module

object CoreDependencyInjection : KoinModuleLoader {
    override val modules: List<Module> =
        DataSourceCoreDependencyInjector.modules +
            DataRepositoryDependencyInjector.modules +
            DomainUseCasesDependencyInjector.modules +
            CoreUiDependencyInjector.modules +
            PresentationViewModelsDependencyInjector.modules
}
