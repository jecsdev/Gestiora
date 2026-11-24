package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.Res
import gestiora.shared.presentation.ui.generated.resources.all_day
import gestiora.shared.presentation.ui.generated.resources.reminder_time_am
import gestiora.shared.presentation.ui.generated.resources.reminder_time_pm
import kotlin.test.Test
import kotlin.test.assertEquals

class ReminderTimeTest {
    @Test fun displaysMidnightAndNoonWithoutChangingStoredTime() {
        val ui = UiCopy(mapOf(
            Res.string.all_day to "All day",
            Res.string.reminder_time_am to "%1\$s:%2\$s AM",
            Res.string.reminder_time_pm to "%1\$s:%2\$s PM",
        ))
        assertEquals("All day", reminderTimeLabel(ui, null))
        assertEquals("12:07 AM", reminderTimeLabel(ui, "00:07"))
        assertEquals("12:40 PM", reminderTimeLabel(ui, "12:40"))
        assertEquals("11:59 PM", reminderTimeLabel(ui, "23:59"))
    }
}
