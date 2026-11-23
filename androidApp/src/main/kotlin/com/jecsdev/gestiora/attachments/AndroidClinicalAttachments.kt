package com.jecsdev.gestiora.attachments

import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.GestioraApplication
import com.jecsdev.gestiora.R
import com.jecsdev.gestiora.data.datasources.core.room.AttachmentCollection
import com.jecsdev.gestiora.data.datasources.core.room.AttachmentStorageLimitException
import com.jecsdev.gestiora.data.datasources.core.room.ClinicalAttachment
import com.jecsdev.gestiora.data.datasources.core.room.ProtectedClinicalStorage
import com.jecsdev.gestiora.presentation.ui.ClinicalAttachmentContent
import com.jecsdev.gestiora.presentation.ui.ClinicalAttachmentKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun rememberClinicalAttachments(app: GestioraApplication, allowImport: Boolean): ClinicalAttachmentContent {
    val flow = retain { AttachmentFlow(app) }
    // Register once above the navigation and the shared RAM-only saveable-state boundary.
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), flow::importFile)
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream"), flow::exportFile)
    return { kind, id, enabled ->
        val collection = when (kind) {
            ClinicalAttachmentKind.Consultation -> AttachmentCollection.Consultation
            ClinicalAttachmentKind.Exam -> AttachmentCollection.Exam
        }
        AttachmentActions(flow, AttachmentTarget(collection, id), enabled, allowImport,
            open = { open.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) },
            create = { create.launch(it) })
    }
}

private data class AttachmentTarget(val collection: AttachmentCollection, val id: String)
private data class AttachmentInfo(val name: String, val mimeType: String)
private enum class AttachmentAction { Import, Export, Delete }

@Composable
private fun AttachmentActions(
    flow: AttachmentFlow,
    target: AttachmentTarget,
    enabled: Boolean,
    allowImport: Boolean,
    open: () -> Unit,
    create: (String) -> Unit,
) {
    var info by retain(target) { mutableStateOf<AttachmentInfo?>(null) }
    var loaded by retain(target) { mutableStateOf(false) }
    var readFailed by retain(target) { mutableStateOf(false) }
    var confirm by retain(target) { mutableStateOf<AttachmentAction?>(null) }
    val result = flow.results[target]
    LaunchedEffect(target, result) {
        loaded = false
        readFailed = false
        try {
            info = flow.readInfo(target)
            loaded = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A read failure must not look like an empty slot that can be overwritten.
            info = null
            readFailed = true
        }
    }
    val existing = info
    val available = enabled && flow.active == null && loaded
    if (existing != null) Text(stringResource(R.string.attachment_name, existing.name))
    if (existing != null || (allowImport && loaded)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (allowImport) TextButton(enabled = available, onClick = {
                if (existing == null) flow.choose(target, AttachmentAction.Import, open)
                else confirm = AttachmentAction.Import
            }) {
                Text(stringResource(if (existing == null) R.string.attachment_add else R.string.attachment_replace))
            }
            if (existing != null) {
                TextButton(enabled = available, onClick = { confirm = AttachmentAction.Export }) {
                    Text(stringResource(R.string.attachment_save_copy))
                }
                TextButton(enabled = available, onClick = { confirm = AttachmentAction.Delete }) {
                    Text(stringResource(R.string.attachment_remove))
                }
            }
        }
    }
    val message = when {
        flow.active == target -> R.string.attachment_working
        readFailed -> R.string.attachment_read_failed
        else -> result?.message
    }
    message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
    val action = confirm
    if (action != null && existing != null) {
        val (title, body) = when (action) {
            AttachmentAction.Import -> R.string.attachment_replace to R.string.attachment_replace_warning
            AttachmentAction.Export -> R.string.attachment_save_copy to R.string.attachment_copy_warning
            AttachmentAction.Delete -> R.string.attachment_remove to R.string.attachment_remove_warning
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(title)) },
            text = { Text(stringResource(body)) },
            confirmButton = {
                TextButton(enabled = available, onClick = {
                    confirm = null
                    when (action) {
                        AttachmentAction.Import -> flow.choose(target, action, open)
                        AttachmentAction.Export -> flow.choose(target, action) {
                            val extension = when (existing.mimeType) {
                                "application/pdf" -> "pdf"
                                "image/jpeg" -> "jpg"
                                else -> "png"
                            }
                            create(existing.name.substringBeforeLast('.').ifBlank { "Gestiora" } + "." + extension)
                        }
                        AttachmentAction.Delete -> flow.remove(target)
                    }
                }) { Text(stringResource(title)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

private class AttachmentResult(val message: Int?)

private class AttachmentFlow(private val app: GestioraApplication) : RetainObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val storage get() = app.graph.koin.get<ProtectedClinicalStorage>()
    private val resolver get() = app.contentResolver
    private var awaiting: AttachmentAction? = null
    var active by mutableStateOf<AttachmentTarget?>(null)
        private set
    val results = mutableStateMapOf<AttachmentTarget, AttachmentResult>()

    suspend fun readInfo(target: AttachmentTarget): AttachmentInfo? = withContext(Dispatchers.IO) {
        storage.readAttachment(target.collection, target.id)?.let {
            try { AttachmentInfo(it.name, it.mimeType) } finally { it.bytes.fill(0) }
        }
    }

    fun choose(target: AttachmentTarget, action: AttachmentAction, launch: () -> Unit) {
        if (active != null) return
        active = target
        awaiting = action
        try {
            launch()
        } catch (_: RuntimeException) {
            awaiting = null
            finish(target, R.string.attachment_failed)
        }
    }

    fun importFile(uri: Uri?) = receive(uri, AttachmentAction.Import) { target, source ->
        val name = resolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }.orEmpty().take(ClinicalAttachment.MAX_NAME)
            .map { if (it.isISOControl() || it == '/' || it == '\\') '_' else it }
            .joinToString("").trim().ifBlank { "Gestiora" }
        val bytes = readBounded(source)
        try {
            storage.writeAttachment(target.collection, target.id, ClinicalAttachment(name, bytes))
        } finally {
            bytes.fill(0)
        }
    }

    fun exportFile(uri: Uri?) = receive(uri, AttachmentAction.Export) { target, destination ->
        try {
            val attachment = checkNotNull(storage.readAttachment(target.collection, target.id))
            try {
                checkNotNull(resolver.openOutputStream(destination, "wt")).use { it.write(attachment.bytes) }
            } finally {
                attachment.bytes.fill(0)
            }
        } catch (failure: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, destination) }
            throw failure
        }
    }

    fun remove(target: AttachmentTarget) {
        if (active != null) return
        active = target
        work(target, AttachmentAction.Delete) { storage.deleteAttachment(target.collection, target.id) }
    }

    private fun receive(uri: Uri?, action: AttachmentAction, block: suspend (AttachmentTarget, Uri) -> Unit) {
        val target = active ?: return
        if (awaiting != action) return
        awaiting = null
        if (uri == null) finish(target, null) else work(target, action) { block(target, uri) }
    }

    private fun work(target: AttachmentTarget, action: AttachmentAction, block: suspend () -> Unit) {
        scope.launch {
            val message = try {
                withContext(Dispatchers.IO) { block() }
                when (action) {
                    AttachmentAction.Import -> R.string.attachment_added
                    AttachmentAction.Export -> R.string.attachment_copy_saved
                    AttachmentAction.Delete -> R.string.attachment_removed
                }
            } catch (cancelled: CancellationException) {
                active = null
                throw cancelled
            } catch (_: AttachmentStorageLimitException) {
                R.string.attachment_storage_limit
            } catch (_: IllegalArgumentException) {
                if (action == AttachmentAction.Import) R.string.attachment_invalid else R.string.attachment_failed
            } catch (_: Exception) {
                R.string.attachment_failed
            }
            finish(target, message)
        }
    }

    private fun finish(target: AttachmentTarget, message: Int?) {
        results[target] = AttachmentResult(message)
        active = null
    }

    private fun readBounded(uri: Uri): ByteArray {
        val buffer = ByteArray(ClinicalAttachment.MAX_BYTES + 1)
        try {
            return checkNotNull(resolver.openInputStream(uri)).use { input ->
                var size = 0
                while (size < buffer.size) {
                    val read = input.read(buffer, size, buffer.size - size)
                    if (read == -1) return@use buffer.copyOf(size)
                    check(read > 0)
                    size += read
                }
                throw IllegalArgumentException("Attachment exceeds size limit")
            }
        } finally {
            buffer.fill(0)
        }
    }

    override fun onRetained() = Unit
    override fun onEnteredComposition() = Unit
    override fun onExitedComposition() = Unit
    override fun onRetired() = scope.cancel()
    override fun onUnused() = scope.cancel()
}
