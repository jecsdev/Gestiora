package com.jecsdev.gestiora.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.StringResource
import gestiora.shared.presentation.ui.generated.resources.*

/** Platform-owned access; Android permission requests only occur after an explicit tap. */
class NotificationControls(
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val needsRetry: Boolean = false,
    val request: () -> Unit = {},
    val timingMessage: StringResource = Res.string.notifications_timing,
    val queueLimitReached: Boolean = false,
)

internal val LocalNotificationControls = staticCompositionLocalOf { NotificationControls() }

@Composable
internal fun ReminderNotificationSettings() {
    val ui = LocalUiCopy.current
    val access = LocalNotificationControls.current
    Text(ui.s(when {
        !access.supported -> Res.string.notifications_unavailable
        !access.enabled -> Res.string.notifications_disabled
        else -> Res.string.notifications_enabled
    }), style = MaterialTheme.typography.bodySmall)
    if (access.supported) {
        Text(ui.s(access.timingMessage), style = MaterialTheme.typography.bodySmall)
        if (access.queueLimitReached) {
            Text(ui.s(Res.string.notifications_queue_limit_ios), style = MaterialTheme.typography.bodySmall)
        }
        if (access.needsRetry) Text(ui.s(Res.string.notifications_retry_message), color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = access.request) {
            Text(ui.s(if (access.needsRetry) Res.string.retry else Res.string.notifications_manage))
        }
    }
}
