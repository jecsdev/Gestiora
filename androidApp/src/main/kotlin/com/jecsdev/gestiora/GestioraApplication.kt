package com.jecsdev.gestiora

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jecsdev.gestiora.core.di.CoreDependencyInjection
import com.jecsdev.gestiora.data.datasources.ScheduleChangeNotifier
import com.jecsdev.gestiora.data.datasources.PREGNANCY_JOURNAL_STORE
import com.jecsdev.gestiora.data.datasources.core.room.ProtectedClinicalStorage
import com.jecsdev.gestiora.data.repository.ProtectedPregnancyJournal
import com.jecsdev.gestiora.domain.repository.AppointmentRepository
import com.jecsdev.gestiora.domain.repository.ExamRecordRepository
import com.jecsdev.gestiora.domain.repository.PersonalReminderRepository
import com.jecsdev.gestiora.reminders.AndroidReminderScheduler
import com.jecsdev.gestiora.reminders.ReminderReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** Activity and background receivers share the same repository locks and Room instances. */
class GestioraApplication : Application() {
    val graph by lazy {
        koinApplication {
            modules(CoreDependencyInjection.modules + module {
                single<Context> { this@GestioraApplication }
                single {
                    val storage = get<ProtectedClinicalStorage>()
                    ProtectedPregnancyJournal(
                        read = { storage.read(PREGNANCY_JOURNAL_STORE) },
                        write = { storage.write(PREGNANCY_JOURNAL_STORE, it) },
                    )
                }
                single<ScheduleChangeNotifier> { ScheduleChangeNotifier {
                    // The OS owns the queued broadcast even when the Activity leaves the foreground.
                    sendBroadcast(Intent(this@GestioraApplication, ReminderReceiver::class.java)
                        .setAction("com.jecsdev.gestiora.REFRESH_REMINDERS"))
                } }
            })
        }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private val scheduler by lazy { AndroidReminderScheduler(this) }
    var reminderRefreshFailed by mutableStateOf(false)
        private set

    fun refreshReminders(deliverDue: Boolean = false, finished: () -> Unit = {}) {
        scope.launch {
            try {
                withTimeout(8_000) {
                    lock.withLock {
                        // Cleanup is gated on verified import/read-back, never on a successful UI save.
                        graph.koin.get<ProtectedClinicalStorage>().finishMigration()
                        val appointments = graph.koin.get<AppointmentRepository>().list()
                        val exams = graph.koin.get<ExamRecordRepository>().list()
                        val personalReminders = graph.koin.get<PersonalReminderRepository>().list()
                        scheduler.reconcile(appointments, exams, personalReminders, deliverDue)
                    }
                }
                withContext(Dispatchers.Main) { reminderRefreshFailed = false }
            } catch (failure: Exception) {
                if (failure is CancellationException && failure !is TimeoutCancellationException) throw failure
                withContext(Dispatchers.Main) { reminderRefreshFailed = true }
                // No empty substitute for unreadable records. Retry locally; never log clinical data.
                runCatching { scheduler.retryLater() }
            } finally { finished() }
        }
    }
}
