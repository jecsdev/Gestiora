package com.jecsdev.gestiora.presentation.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.data.datasources.core.room.PortableBackupPayload
import com.jecsdev.gestiora.data.datasources.core.room.ProtectedClinicalStorage
import com.jecsdev.gestiora.data.datasources.core.room.UnsupportedPortableBackupVersion
import gestiora.shared.presentation.ui.generated.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.cinterop.ExperimentalForeignApi
import org.jetbrains.compose.resources.StringResource
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalForeignApi::class)
internal class IosBackupFlow(
    private val scope: CoroutineScope,
    private val crypto: IosClinicalCrypto,
    private val storage: () -> ProtectedClinicalStorage,
    private val onRestore: () -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    private enum class Step { Idle, SelectingFile, Password, Working, SavingFile, Message }
    private var step by mutableStateOf(Step.Idle)
    private var message by mutableStateOf<StringResource?>(null)
    private var source: NSURL? = null
    private var stagedExport: NSURL? = null
    private var restored by mutableStateOf(false)

    fun controls(controller: UIViewController) = BackupControls(
        canRestore = { withContext(Dispatchers.Default) { storage().canRestoreBackup() } },
        restored = restored,
        restoreAction = {
            val ui = LocalUiCopy.current
            TextButton(
                onClick = { selectBackup(controller) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(ui.s(Res.string.backup_restore)) }
            Dialog(controller, exporting = false)
        },
        exportAction = {
            val ui = LocalUiCopy.current
            OutlinedButton(
                onClick = { if (step == Step.Idle) step = Step.Password },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(ui.s(Res.string.backup_export)) }
            Dialog(controller, exporting = true)
        },
    )

    private fun selectBackup(controller: UIViewController) {
        if (step != Step.Idle) return
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeData), asCopy = false)
        picker.delegate = this
        step = Step.SelectingFile
        controller.presentViewController(picker, animated = true, completion = null)
    }

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        when (step) {
            Step.SelectingFile -> {
                source = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                if (source == null) dismiss() else step = Step.Password
            }
            Step.SavingFile -> {
                removeStagedExport()
                show(Res.string.backup_saved)
            }
            else -> Unit
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        removeStagedExport()
        dismiss()
    }

    private fun submit(controller: UIViewController, password: ByteArray, exporting: Boolean) {
        step = Step.Working
        scope.launch {
            try {
                if (exporting) {
                    val file = withContext(Dispatchers.Default) { encryptSnapshot(password) }
                    exportFile(controller, file)
                } else {
                    val url = checkNotNull(source)
                    val outcome = withContext(Dispatchers.Default) { restoreBackup(url, password) }
                    if (outcome == null) {
                        dismiss()
                        restored = true
                        onRestore()
                    } else if (outcome == Res.string.backup_auth_failed) {
                        message = outcome
                        step = Step.Password
                    } else show(outcome)
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: UnsupportedPortableBackupVersion) {
                show(Res.string.backup_unsupported_version)
            } catch (_: Exception) {
                removeStagedExport()
                show(if (exporting) Res.string.backup_operation_failed else Res.string.backup_invalid)
            } finally {
                password.fill(0)
            }
        }
    }

    private suspend fun encryptSnapshot(password: ByteArray): NSData {
        val snapshot = storage().backupSnapshot()
        try {
            val (version, plain) = PortableBackupPayload.encode(snapshot)
            try {
                return checkNotNull(crypto.encryptBackup(plain.toData(), password.toData(), version))
            } finally {
                plain.fill(0)
            }
        } finally {
            snapshot.clearAttachments()
        }
    }

    private suspend fun restoreBackup(url: NSURL, password: ByteArray): StringResource? {
        val store = storage()
        if (!store.canRestoreBackup()) return Res.string.backup_restore_blocked
        val file = crypto.readBackup(url, PortableBackupPayload.MAX_FILE) ?: return Res.string.backup_invalid
        val version = PortableBackupPayload.validateFile(file.toBytes())
        val plain = crypto.decryptBackup(file, password.toData())?.toBytes() ?: return Res.string.backup_auth_failed
        val backup = try { PortableBackupPayload.decode(plain, version) } finally { plain.fill(0) }
        return try {
            kotlin.coroutines.coroutineContext.ensureActive()
            if (store.restoreBackup(backup)) null else Res.string.backup_restore_blocked
        } finally {
            backup.clearAttachments()
        }
    }

    private fun exportFile(controller: UIViewController, encrypted: NSData) {
        val directory = NSURL.fileURLWithPath(NSTemporaryDirectory() + NSUUID().UUIDString, isDirectory = true)
        check(NSFileManager.defaultManager.createDirectoryAtURL(directory, false, null, null))
        stagedExport = directory
        val file = checkNotNull(directory.URLByAppendingPathComponent("Gestiora.gestiora"))
        check(crypto.writeBackup(encrypted, file))
        val picker = UIDocumentPickerViewController(forExportingURLs = listOf(file), asCopy = true)
        picker.delegate = this
        step = Step.SavingFile
        controller.presentViewController(picker, animated = true, completion = null)
    }

    private fun removeStagedExport() {
        stagedExport?.let { NSFileManager.defaultManager.removeItemAtURL(it, null) }
        stagedExport = null
    }

    private fun show(resource: StringResource) {
        source = null
        message = resource
        step = Step.Message
    }

    private fun dismiss() {
        source = null
        message = null
        step = Step.Idle
    }

    @Composable
    private fun Dialog(controller: UIViewController, exporting: Boolean) {
        val ui = LocalUiCopy.current
        val title = ui.s(if (exporting) Res.string.backup_export else Res.string.backup_restore)
        when (step) {
            Step.Password -> {
                var password by remember { mutableStateOf("") }
                var confirmation by remember { mutableStateOf("") }
                val ready = if (exporting) password.length >= 15 && password == confirmation else password.isNotEmpty()
                AlertDialog(
                    onDismissRequest = ::dismiss,
                    title = { Text(title) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (exporting) Text(ui.s(Res.string.backup_password_help))
                            BackupPasswordField(password, { password = it }, Res.string.backup_password)
                            if (exporting) {
                                BackupPasswordField(confirmation, { confirmation = it }, Res.string.backup_password_confirm)
                            }
                            message?.let { Text(ui.s(it)) }
                        }
                    },
                    confirmButton = {
                        TextButton(enabled = ready, onClick = {
                            val encoded = password.encodeToByteArray()
                            password = ""
                            confirmation = ""
                            submit(controller, encoded, exporting)
                        }) { Text(ui.s(Res.string.ok)) }
                    },
                    dismissButton = { TextButton(onClick = ::dismiss) { Text(ui.s(Res.string.cancel)) } },
                )
            }
            Step.Working -> AlertDialog(
                onDismissRequest = {},
                title = { Text(title) },
                text = { CircularProgressIndicator() },
                confirmButton = {},
            )
            Step.Message -> AlertDialog(
                onDismissRequest = ::dismiss,
                title = { Text(title) },
                text = { message?.let { Text(ui.s(it)) } },
                confirmButton = { TextButton(onClick = ::dismiss) { Text(ui.s(Res.string.ok)) } },
            )
            else -> Unit
        }
    }
}

@Composable
private fun BackupPasswordField(value: String, onChange: (String) -> Unit, label: StringResource) {
    val ui = LocalUiCopy.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(ui.s(label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}
