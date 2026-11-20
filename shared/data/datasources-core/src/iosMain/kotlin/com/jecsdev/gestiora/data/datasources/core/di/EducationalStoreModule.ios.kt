package com.jecsdev.gestiora.data.datasources.core.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.jecsdev.gestiora.data.datasources.EducationalStore
import com.jecsdev.gestiora.data.datasources.core.education.EducationalDatabase
import com.jecsdev.gestiora.data.datasources.core.education.RoomEducationalStore
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

internal actual fun educationalStoreModule(): Module = module {
    single {
        // Library exists in the app sandbox and persists across app launches (unlike Caches).
        Room.databaseBuilder<EducationalDatabase>(name = NSHomeDirectory() + "/Library/gestiora_education.db")
            .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.Default).build()
    }
    single<EducationalStore> { RoomEducationalStore(get<EducationalDatabase>().catalog()) }
}
