package com.jecsdev.gestiora.core.common.di

import org.koin.core.module.Module

interface KoinModuleLoader {
    val modules: List<Module>
}
