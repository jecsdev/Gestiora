package com.jecsdev.gestiora.data.datasources.core.room

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File

internal fun clinicalDatabase(context: Context): ClinicalDatabase = Room.databaseBuilder<ClinicalDatabase>(
    context.applicationContext,
    File(context.noBackupFilesDir, "gestiora_clinical.db").absolutePath,
).setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()

// Existing contracts are synchronous; repositories already invoke them off the main thread.
// Keep this bridge platform-only until a separate suspend-contract migration is warranted.
internal actual fun <T> storageCall(block: suspend () -> T): T {
    check(Looper.myLooper() != Looper.getMainLooper()) { "Clinical storage must run off the main thread" }
    return runBlocking(Dispatchers.IO) { block() }
}
