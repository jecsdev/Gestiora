package com.jecsdev.gestiora.data.datasources.core.di

import com.jecsdev.gestiora.data.datasources.ThemePreferenceStore
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

internal actual fun themePreferenceModule(): Module = module {
    single<ThemePreferenceStore> { IosThemePreferenceStore() }
}

private class IosThemePreferenceStore : ThemePreferenceStore {
    private val preferences = NSUserDefaults.standardUserDefaults
    override fun read(): String? = preferences.stringForKey("gestiora.display.theme")
    override fun write(value: String) {
        preferences.setObject(value, forKey = "gestiora.display.theme")
    }
}
