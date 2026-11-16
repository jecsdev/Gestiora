package com.jecsdev.gestiora.data.datasources.core.di

import android.content.Context
import com.jecsdev.gestiora.data.datasources.ThemePreferenceStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun themePreferenceModule(): Module = module {
    single<ThemePreferenceStore> { AndroidThemePreferenceStore(get<Context>()) }
}

private class AndroidThemePreferenceStore(context: Context) : ThemePreferenceStore {
    private val preferences = context.applicationContext.getSharedPreferences("gestiora_display", Context.MODE_PRIVATE)
    override fun read(): String? = preferences.getString("theme", null)
    override fun write(value: String) {
        check(preferences.edit().putString("theme", value).commit()) { "Theme preference save failed" }
    }
}
