package com.jecsdev.gestiora.presentation.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jecsdev.gestiora.data.datasources.ScheduleChangeNotifier
import com.jecsdev.gestiora.domain.models.AppointmentReminder
import com.jecsdev.gestiora.domain.models.AppointmentStatus
import com.jecsdev.gestiora.domain.models.ExamStatus
import com.jecsdev.gestiora.domain.models.ReminderKind
import com.jecsdev.gestiora.domain.repository.AppointmentRepository
import com.jecsdev.gestiora.domain.repository.ExamRecordRepository
import com.jecsdev.gestiora.domain.repository.PersonalReminderRepository
import gestiora.shared.presentation.ui.generated.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.koin.core.Koin
import platform.Foundation.*
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

private const val REMINDER_PREFIX = "gestiora-reminder-"
private const val REMINDER_CATEGORY = "gestiora-local-reminder"
private const val MAX_PENDING_REMINDERS = 64

internal class IosReminderScheduler(private val scope: CoroutineScope) : ScheduleChangeNotifier {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val reconcileLock = Mutex()
    private var graph: Koin? = null
    private var enabled by mutableStateOf(false)
    private var needsRetry by mutableStateOf(false)
    private var queueLimitReached by mutableStateOf(false)

    fun start(graph: Koin) {
        this.graph = graph
        requestReconcile()
    }

    fun stop() {
        graph = null
    }

    fun controls() = NotificationControls(
        supported = true,
        enabled = enabled,
        needsRetry = needsRetry,
        request = ::manageNotifications,
        timingMessage = Res.string.notifications_timing_ios,
        queueLimitReached = enabled && queueLimitReached,
    )

    private fun manageNotifications() {
        if (needsRetry) {
            requestReconcile()
            return
        }
        scope.launch {
            val status = withTimeoutOrNull(5_000) { authorizationStatus() }
            if (status == null) {
                needsRetry = true
            } else if (status == UNAuthorizationStatusNotDetermined) {
                center.requestAuthorizationWithOptions(
                    UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
                ) { _, _ -> requestReconcile() }
            } else {
                NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
                    UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
                }
            }
        }
    }

    override fun requestReconcile() {
        scope.launch {
            reconcileLock.withLock {
                val currentGraph = graph ?: return@withLock
                try {
                    withTimeout(10_000) {
                        val status = checkNotNull(authorizationStatus()) { "Notification settings unavailable" }
                        enabled = status == UNAuthorizationStatusAuthorized ||
                            status == UNAuthorizationStatusProvisional ||
                            status == UNAuthorizationStatusEphemeral
                        if (enabled) reconcile(currentGraph)
                    }
                    needsRetry = false
                } catch (failure: Exception) {
                    if (failure is CancellationException && failure !is TimeoutCancellationException) throw failure
                    needsRetry = true
                }
            }
        }
    }

    private suspend fun authorizationStatus(): UNAuthorizationStatus? = suspendCancellableCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { continuation.resume(it?.authorizationStatus) }
    }

    private suspend fun reconcile(graph: Koin) {
        val plan = reminderPlan(graph)
        val byId = plan.associateBy { it.identifier }
        val now = NSDate().timeIntervalSince1970
        val upcoming = plan.filter { it.dueAt.timeIntervalSince1970 > now }
        queueLimitReached = upcoming.size > MAX_PENDING_REMINDERS
        val future = upcoming.sortedBy { it.dueAt.timeIntervalSince1970 }.take(MAX_PENDING_REMINDERS)
        val futureIds = future.mapTo(mutableSetOf()) { it.identifier }
        val pending = suspendCancellableCoroutine<List<UNNotificationRequest>> { continuation ->
            center.getPendingNotificationRequestsWithCompletionHandler { requests ->
                continuation.resume(requests.orEmpty().filterIsInstance<UNNotificationRequest>())
            }
        }
        center.removePendingNotificationRequestsWithIdentifiers(
            pending.map { it.identifier }.filter { it.startsWith(REMINDER_PREFIX) && it !in futureIds },
        )
        val delivered = suspendCancellableCoroutine<List<UNNotification>> { continuation ->
            center.getDeliveredNotificationsWithCompletionHandler { notifications ->
                continuation.resume(notifications.orEmpty().filterIsInstance<UNNotification>())
            }
        }
        center.removeDeliveredNotificationsWithIdentifiers(
            delivered.map { it.request.identifier }.filter {
                it.startsWith(REMINDER_PREFIX) && (it !in byId || it in futureIds)
            },
        )
        if (future.isEmpty()) return

        val hiddenPreview = getString(Res.string.notification_preview_hidden)
        center.setNotificationCategories(setOf(UNNotificationCategory.categoryWithIdentifier(
            REMINDER_CATEGORY, emptyList<Any>(), emptyList<Any>(), hiddenPreview, 0u,
        )))
        val pendingById = pending.associateBy { it.identifier }
        for (item in future) {
            val title = getString(item.title)
            val existing = pendingById[item.identifier]
            val previousDate = (existing?.trigger as? UNCalendarNotificationTrigger)?.nextTriggerDate()
            if (previousDate?.timeIntervalSince1970 == item.dueAt.timeIntervalSince1970 &&
                existing.content.title == title && existing.content.body == item.body
            ) continue
            if (item.dueAt.timeIntervalSince1970 <= NSDate().timeIntervalSince1970) continue

            val components = NSCalendar.currentCalendar.components(
                NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                    NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond,
                fromDate = item.dueAt,
            )
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(item.body)
                setCategoryIdentifier(REMINDER_CATEGORY)
                setSound(UNNotificationSound.defaultSound)
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
            val request = UNNotificationRequest.requestWithIdentifier(item.identifier, content, trigger)
            val added = suspendCancellableCoroutine<Boolean> { continuation ->
                center.addNotificationRequest(request) { continuation.resume(it == null) }
            }
            check(added) { "Could not schedule a local notification" }
        }
    }
}

private data class IosPlannedReminder(
    val identifier: String,
    val dueAt: NSDate,
    val title: StringResource,
    val body: String,
)

private suspend fun reminderPlan(graph: Koin): List<IosPlannedReminder> {
    val appointments = graph.get<AppointmentRepository>().list()
    val exams = graph.get<ExamRecordRepository>().list()
    val reminders = graph.get<PersonalReminderRepository>().list()
    val iso = NSDateFormatter().apply {
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
        calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
        dateFormat = "yyyy-MM-dd HH:mm"
        lenient = true
    }
    val display = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
    }
    val calendar = NSCalendar.currentCalendar

    return buildList {
        for (item in appointments) {
            if (item.status != AppointmentStatus.SCHEDULED || item.reminder == AppointmentReminder.NONE) continue
            val event = iso.dateFromString("${item.date} ${item.time}") ?: continue
            val days = when (item.reminder) {
                AppointmentReminder.ONE_DAY -> -1L
                AppointmentReminder.TWO_DAYS -> -2L
                AppointmentReminder.ONE_WEEK -> -7L
                else -> 0L
            }
            val due = if (days == 0L) event.dateByAddingTimeInterval(-3600.0)
                else calendar.dateByAddingUnit(NSCalendarUnitDay, days, event, 0u) ?: continue
            val body = listOf(item.doctor, display.stringFromDate(event)).filter { it.isNotBlank() }.joinToString(" · ")
            add(IosPlannedReminder("${REMINDER_PREFIX}appointment-${item.id}", due, Res.string.local_appointment_alert, body))
        }
        for (item in exams) {
            if (item.status != ExamStatus.SCHEDULED || !item.reminder || item.scheduled == null) continue
            val event = iso.dateFromString("${item.scheduled} 09:00") ?: continue
            val due = calendar.dateByAddingUnit(NSCalendarUnitDay, -1, event, 0u) ?: continue
            add(IosPlannedReminder("${REMINDER_PREFIX}exam-${item.id}", due, Res.string.local_exam_alert, item.name))
        }
        for (item in reminders) {
            if (item.done) continue
            val due = iso.dateFromString("${item.date} ${item.time ?: "09:00"}") ?: continue
            val title = if (item.kind == ReminderKind.MEDICATION) Res.string.local_medication_alert
                else Res.string.local_personal_alert
            add(IosPlannedReminder("${REMINDER_PREFIX}personal-${item.id}", due, title, item.title))
        }
    }
}
