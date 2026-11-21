package com.jecsdev.gestiora

import android.os.Bundle
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.jecsdev.gestiora.backup.rememberBackupControls
import com.jecsdev.gestiora.presentation.ui.App
import com.jecsdev.gestiora.presentation.ui.NotificationControls
import com.jecsdev.gestiora.reminders.AndroidReminderScheduler

class MainActivity : ComponentActivity() {
    private var resumed by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        resumed++
        AndroidReminderScheduler.createChannel(this)
        (application as GestioraApplication).refreshReminders()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val app = application as GestioraApplication
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                resumed++
                app.refreshReminders()
            }
            val enabled = remember(resumed) { AndroidReminderScheduler.notificationsEnabled(this) }
            App(applicationGraph = app.graph, backupControls = rememberBackupControls(app), notificationControls = NotificationControls(
                supported = true,
                enabled = enabled,
                needsRetry = app.reminderRefreshFailed,
                request = {
                    app.refreshReminders()
                    if (!app.reminderRefreshFailed) {
                        val preferences = getSharedPreferences("notification_access", MODE_PRIVATE)
                        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && !preferences.getBoolean("requested", false)) {
                            preferences.edit().putBoolean("requested", true).apply()
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                    }
                },
            ), nativeAd = nativeAdContent)
        }
    }
}
