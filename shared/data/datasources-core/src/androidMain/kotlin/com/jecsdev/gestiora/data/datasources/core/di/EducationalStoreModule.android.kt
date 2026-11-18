package com.jecsdev.gestiora.data.datasources.core.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.jecsdev.gestiora.data.datasources.EducationalStore
import com.jecsdev.gestiora.data.datasources.core.education.EducationalDatabase
import com.jecsdev.gestiora.data.datasources.core.education.RoomEducationalStore
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

internal actual fun educationalStoreModule(): Module = module {
    single {
        val context = get<Context>().applicationContext
        Room.databaseBuilder<EducationalDatabase>(context, File(context.noBackupFilesDir, "gestiora_education.db").absolutePath)
            .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
    }
    single<EducationalStore> { RoomEducationalStore(get<EducationalDatabase>().catalog()) }
}
