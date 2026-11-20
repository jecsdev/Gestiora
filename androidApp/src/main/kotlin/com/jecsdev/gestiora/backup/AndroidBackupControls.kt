package com.jecsdev.gestiora.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.GestioraApplication
import com.jecsdev.gestiora.R
import com.jecsdev.gestiora.data.datasources.core.room.ProtectedClinicalStorage
import com.jecsdev.gestiora.presentation.ui.BackupControls
import javax.crypto.BadPaddingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun rememberBackupControls(app: GestioraApplication): BackupControls {
    val flow = retain { BackupFlow(app) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), flow::selectedBackup)
    val create = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
        flow::selectedDestination,
    )
    LaunchedEffect(flow.state) {
        if (flow.state == BackupStep.ChooseDestination) flow.chooseDestination { create.launch("Gestiora.gestiora") }
    }
    return BackupControls(
        canRestore = flow::canRestore,
        restored = flow.restored,
        restoreAction = {
            TextButton(onClick = { flow.chooseBackup { open.launch(arrayOf("*/*")) } }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.backup_restore))
            }
            BackupDialog(flow, exporting = false)
        },
        exportAction = {
            OutlinedButton(onClick = flow::startExport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.backup_export))
            }
            BackupDialog(flow, exporting = true)
        },
    )
}

private enum class BackupStep { Idle, WaitingForBackup, Password, Working, ChooseDestination, WaitingForDestination, Message }

/** File selection state and passwords live in RAM only, including during Activity recreation. */
private class BackupFlow(private val app: GestioraApplication) : RetainObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val storage get() = app.graph.koin.get<ProtectedClinicalStorage>()
    private val resolver get() = app.contentResolver
    private var source: Uri? = null
    private var encryptedFile: ByteArray? = null
    var state by mutableStateOf(BackupStep.Idle)
        private set
    var message by mutableStateOf<Int?>(null)
        private set
    var restored by mutableStateOf(false)
        private set

    suspend fun canRestore(): Boolean = withContext(Dispatchers.IO) { storage.canRestoreBackup() }

    fun chooseBackup(launch: () -> Unit) {
        if (state != BackupStep.Idle) return
        state = BackupStep.WaitingForBackup
        try {
            launch()
        } catch (_: RuntimeException) {
            show(R.string.backup_operation_failed)
        }
    }

    fun selectedBackup(uri: Uri?) {
        if (state != BackupStep.WaitingForBackup) return
        if (uri == null) {
            dismiss()
            return
        }
        source = uri
        state = BackupStep.Password
    }

    fun startExport() {
        if (state == BackupStep.Idle) state = BackupStep.Password
    }

    fun chooseDestination(launch: () -> Unit) {
        state = BackupStep.WaitingForDestination
        try {
            launch()
        } catch (_: RuntimeException) {
            encryptedFile = null
            show(R.string.backup_operation_failed)
        }
    }

    fun selectedDestination(uri: Uri?) {
        if (state != BackupStep.WaitingForDestination) return
        val file = encryptedFile
        encryptedFile = null
        if (uri == null || file == null) {
            dismiss()
            return
        }
        state = BackupStep.Working
        scope.launch {
            try {
                withContext(Dispatchers.IO) { writeDocument(resolver, uri, file) }
                show(R.string.backup_saved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                show(R.string.backup_operation_failed)
            }
        }
    }

    fun submit(password: CharArray, exporting: Boolean) {
        state = BackupStep.Working
        message = null
        scope.launch {
            try {
                if (exporting) {
                    encryptedFile = withContext(Dispatchers.IO) {
                        BackupFile.encrypt(storage.backupSnapshot(), password)
                    }
                    state = BackupStep.ChooseDestination
                } else {
                    val uri = checkNotNull(source)
                    val applied = withContext(Dispatchers.IO) {
                        if (!storage.canRestoreBackup()) return@withContext false
                        val payloads = checkNotNull(resolver.openInputStream(uri)).use {
                            BackupFile.decrypt(it, password)
                        }
                        ensureActive()
                        storage.restoreBackup(payloads)
                    }
                    if (applied) {
                        dismiss()
                        restored = true
                        app.refreshReminders()
                    } else show(R.string.backup_restore_blocked)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: UnsupportedBackupVersion) {
                show(R.string.backup_unsupported_version)
            } catch (_: BadPaddingException) {
                message = R.string.backup_auth_failed
                state = BackupStep.Password
            } catch (_: Exception) {
                show(if (exporting) R.string.backup_operation_failed else R.string.backup_invalid)
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun dismiss() {
        source = null
        message = null
        state = BackupStep.Idle
    }

    private fun show(text: Int) {
        source = null
        message = text
        state = BackupStep.Message
    }

    override fun onRetained() = Unit
    override fun onEnteredComposition() = Unit
    override fun onExitedComposition() = Unit
    override fun onRetired() = scope.cancel()
    override fun onUnused() = scope.cancel()
}

private fun writeDocument(resolver: ContentResolver, uri: Uri, file: ByteArray) {
    try {
        checkNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(file) }
    } catch (failure: Exception) {
        // CreateDocument returned a newly created destination. Remove an incomplete export if supported.
        runCatching { DocumentsContract.deleteDocument(resolver, uri) }
        throw failure
    }
}

@Composable
private fun BackupDialog(flow: BackupFlow, exporting: Boolean) {
    val title = stringResource(if (exporting) R.string.backup_export else R.string.backup_restore)
    when (flow.state) {
        BackupStep.Password -> BackupPasswordDialog(title, flow, exporting)
        BackupStep.Working -> AlertDialog(
            onDismissRequest = {},
            title = { Text(title) },
            text = { CircularProgressIndicator() },
            confirmButton = {},
        )
        BackupStep.Message -> AlertDialog(
            onDismissRequest = flow::dismiss,
            title = { Text(title) },
            text = { flow.message?.let { Text(stringResource(it)) } },
            confirmButton = { TextButton(onClick = flow::dismiss) { Text(stringResource(android.R.string.ok)) } },
        )
        else -> Unit
    }
}

@Composable
private fun BackupPasswordDialog(title: String, flow: BackupFlow, exporting: Boolean) {
    // Keep typed passwords out of saved instance state and discard them on dismissal.
    var password by retain { mutableStateOf("") }
    var confirmation by retain { mutableStateOf("") }
    val ready = if (exporting) password.length >= 15 && password == confirmation else password.isNotEmpty()
    val dismiss = {
        password = ""
        confirmation = ""
        flow.dismiss()
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (exporting) Text(stringResource(R.string.backup_password_help))
                BackupPasswordField(password, { password = it }, R.string.backup_password)
                if (exporting) BackupPasswordField(confirmation, { confirmation = it }, R.string.backup_password_confirm)
                flow.message?.let { Text(stringResource(it)) }
            }
        },
        confirmButton = {
            TextButton(enabled = ready, onClick = {
                val secret = password.toCharArray()
                password = ""
                confirmation = ""
                flow.submit(secret, exporting)
            }) { Text(stringResource(if (exporting) R.string.backup_choose_destination else R.string.backup_restore)) }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun BackupPasswordField(value: String, onChange: (String) -> Unit, label: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 512) onChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation(),
    )
}
