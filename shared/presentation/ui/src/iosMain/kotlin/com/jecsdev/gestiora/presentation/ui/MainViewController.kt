package com.jecsdev.gestiora.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.jecsdev.gestiora.core.di.CoreDependencyInjection
import com.jecsdev.gestiora.data.datasources.ScheduleChangeNotifier
import com.jecsdev.gestiora.data.datasources.PREGNANCY_JOURNAL_STORE
import com.jecsdev.gestiora.data.datasources.core.room.ProtectedClinicalStorage
import com.jecsdev.gestiora.data.datasources.core.room.iosClinicalStorageModule
import com.jecsdev.gestiora.data.repository.ProtectedPregnancyJournal
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationSignificantTimeChangeNotification
import org.koin.dsl.module

fun MainViewController(databasePath: String, crypto: IosClinicalCrypto) =
    ComposeUIViewController { IosApp(databasePath, crypto) }

@Composable
private fun IosApp(databasePath: String, crypto: IosClinicalCrypto) {
    val scope = retainedOperationScope()
    val reminders = retain { IosReminderScheduler(scope) }
    val graph = retain {
        val storage = iosClinicalStorageModule(databasePath) { hasCiphertext ->
            IosClinicalCipher(crypto, hasCiphertext)
        }
        RetainedAppGraph(CoreDependencyInjection.modules + storage + module {
            single<ScheduleChangeNotifier> { reminders }
        })
    }.application
    val controller = LocalUIViewController.current
    val backup = retain { IosBackupFlow(scope, crypto, { graph.koin.get() }, reminders::requestReconcile) }
    val journal = retain {
        val storage = graph.koin.get<ProtectedClinicalStorage>()
        val repository = ProtectedPregnancyJournal(
            read = { storage.read(PREGNANCY_JOURNAL_STORE) },
            write = { storage.write(PREGNANCY_JOURNAL_STORE, it) },
        )
        PregnancyJournalControls(repository::list, repository::save, repository::delete)
    }
    DisposableEffect(reminders, graph) {
        val center = NSNotificationCenter.defaultCenter
        fun observe(name: String?) = center.addObserverForName(requireNotNull(name), null, NSOperationQueue.mainQueue) {
            reminders.requestReconcile()
        }
        val active = observe(UIApplicationDidBecomeActiveNotification)
        val timeChanged = observe(UIApplicationSignificantTimeChangeNotification)
        reminders.start(graph.koin)
        onDispose {
            center.removeObserver(active)
            center.removeObserver(timeChanged)
            reminders.stop()
        }
    }
    App(
        applicationGraph = graph,
        notificationControls = reminders.controls(),
        backupControls = backup.controls(controller),
        pregnancyJournal = journal,
    )
}
