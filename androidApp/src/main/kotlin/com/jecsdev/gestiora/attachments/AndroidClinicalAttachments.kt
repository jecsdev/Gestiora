package com.jecsdev.gestiora.attachments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
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
import java.nio.ByteBuffer
import java.io.ByteArrayInputStream

@Composable
internal fun rememberClinicalAttachments(app: GestioraApplication, allowImport: Boolean): ClinicalAttachmentContent {
    val flow = retain { AttachmentFlow(app) }
    // Register once above the navigation and the shared RAM-only saveable-state boundary.
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), flow::importFile)
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream"), flow::exportFile)
    return { kind, id, enabled, showImages ->
        val collection = when (kind) {
            ClinicalAttachmentKind.Consultation -> AttachmentCollection.Consultation
            ClinicalAttachmentKind.Exam -> AttachmentCollection.Exam
            ClinicalAttachmentKind.Journal -> AttachmentCollection.Journal
        }
        val target = AttachmentTarget(collection, id)
        AttachmentActions(flow, target, enabled, allowImport,
            open = {
                val types = when {
                    target.imageOnly -> imageTypes
                    showImages -> arrayOf("application/pdf")
                    else -> arrayOf("application/pdf", *imageTypes)
                }
                open.launch(types)
            },
            create = { create.launch(it) },
            documentOnly = showImages,
        )
        if (showImages) {
            ImageAttachments(flow, target, enabled, allowImport) { open.launch(imageTypes) }
        }
    }
}

private val imageTypes = arrayOf("image/jpeg", "image/png")

private data class AttachmentTarget(val collection: AttachmentCollection, val id: String, val imageSlot: Int? = null) {
    val imageOnly get() = imageSlot != null || collection == AttachmentCollection.Journal
}
private data class AttachmentInfo(val name: String, val mimeType: String)
private enum class AttachmentAction { Import, Export, Delete }

@Composable
private fun ImageAttachments(
    flow: AttachmentFlow,
    target: AttachmentTarget,
    enabled: Boolean,
    allowImport: Boolean,
    open: () -> Unit,
) {
    val change = flow.imageResults[target]
    val slots by produceState<Pair<AttachmentResult?, Result<List<Int>>>?>(null, target, change) {
        val result = try {
            Result.success(flow.imageSlots(target))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(failure)
        }
        value = change to result
    }
    val occupied = slots?.second?.getOrNull()
    HorizontalDivider()
    Text(stringResource(R.string.attachment_images), style = MaterialTheme.typography.titleSmall)
    occupied.orEmpty().forEach { slot ->
        key(slot) {
            Column {
                AttachmentActions(
                    flow, target.copy(imageSlot = slot), enabled, allowImport,
                    open = open, create = {}, showFeedback = false,
                )
            }
        }
    }
    if (allowImport && occupied != null && slots?.first == change) {
        val free = (1..AttachmentCollection.MAX_IMAGES_PER_RECORD).firstOrNull { it !in occupied }
        if (free != null) {
            TextButton(enabled = enabled && flow.active == null, onClick = {
                flow.choose(target.copy(imageSlot = free), AttachmentAction.Import, open)
            }) {
                Text(stringResource(R.string.attachment_add_image))
            }
        } else {
            Text(stringResource(R.string.attachment_images_limit, AttachmentCollection.MAX_IMAGES_PER_RECORD),
                style = MaterialTheme.typography.bodySmall)
        }
    }
    val message = when {
        flow.active?.let { it.imageSlot != null && it.copy(imageSlot = null) == target } == true ->
            R.string.attachment_working
        slots?.second?.isFailure == true -> R.string.attachment_read_failed
        else -> change?.message
    }
    message?.let {
        val label = if (it == R.string.attachment_invalid) R.string.attachment_invalid_image else it
        Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AttachmentActions(
    flow: AttachmentFlow,
    target: AttachmentTarget,
    enabled: Boolean,
    allowImport: Boolean,
    open: () -> Unit,
    create: (String) -> Unit,
    documentOnly: Boolean = false,
    showFeedback: Boolean = true,
) {
    var info by retain(target) { mutableStateOf<AttachmentInfo?>(null) }
    var loaded by retain(target) { mutableStateOf(false) }
    var readFailed by retain(target) { mutableStateOf(false) }
    var confirm by retain(target) { mutableStateOf<AttachmentAction?>(null) }
    var showImage by retain(target) { mutableStateOf(false) }
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
    if (existing != null || (allowImport && loaded && target.imageSlot == null)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (allowImport) TextButton(enabled = available, onClick = {
                if (existing == null) flow.choose(target, AttachmentAction.Import, open)
                else confirm = AttachmentAction.Import
            }) {
                Text(stringResource(when {
                    existing != null -> R.string.attachment_replace
                    target.imageOnly -> R.string.attachment_add_image
                    documentOnly -> R.string.attachment_add_document
                    else -> R.string.attachment_add
                }))
            }
            if (existing != null) {
                if (existing.mimeType.startsWith("image/")) {
                    TextButton(enabled = available, onClick = { showImage = true }) {
                        Text(stringResource(R.string.attachment_view_image))
                    }
                }
                if (!target.imageOnly) {
                    TextButton(enabled = available, onClick = { confirm = AttachmentAction.Export }) {
                        Text(stringResource(R.string.attachment_save_copy))
                    }
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
    message?.takeIf { showFeedback || readFailed }?.let {
        val label = if (target.imageOnly && it == R.string.attachment_invalid) R.string.attachment_invalid_image else it
        Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
    }
    if (showImage && existing != null) {
        AttachmentImageDialog(existing.name, { flow.readImage(target) }, onClose = { showImage = false })
    }
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

@Composable
private fun AttachmentImageDialog(name: String, load: suspend () -> Bitmap, onClose: () -> Unit) {
    val image by produceState<Result<Bitmap>?>(null, name) {
        value = try {
            Result.success(load())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(failure)
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.attachment_view_image)) },
        text = {
            val bitmap = image?.getOrNull()
            when {
                image == null -> CircularProgressIndicator()
                bitmap == null -> Text(stringResource(R.string.attachment_read_failed))
                else -> Image(
                    bitmap.asImageBitmap(), contentDescription = name,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.attachment_close_image)) } },
    )
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
    val imageResults = mutableStateMapOf<AttachmentTarget, AttachmentResult>()

    suspend fun imageSlots(target: AttachmentTarget): List<Int> = withContext(Dispatchers.IO) {
        storage.imageSlots(target.collection, target.id)
    }

    suspend fun readInfo(target: AttachmentTarget): AttachmentInfo? = withContext(Dispatchers.IO) {
        storage.readAttachment(target.collection, target.id, target.imageSlot)?.let {
            try { AttachmentInfo(it.name, it.mimeType) } finally { it.bytes.fill(0) }
        }
    }

    suspend fun readImage(target: AttachmentTarget): Bitmap = withContext(Dispatchers.IO) {
        val attachment = checkNotNull(storage.readAttachment(target.collection, target.id, target.imageSlot))
        try {
            require(attachment.mimeType.startsWith("image/"))
            decodePreview(attachment.bytes)
        } finally {
            attachment.bytes.fill(0)
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
            val attachment = ClinicalAttachment(name, bytes)
            if (target.imageOnly) {
                require(attachment.mimeType.startsWith("image/"))
                try {
                    decodePreview(bytes).recycle()
                } catch (failure: Exception) {
                    throw IllegalArgumentException("Image cannot be decoded", failure)
                }
            }
            storage.writeAttachment(target.collection, target.id, attachment, target.imageSlot)
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
        work(target, AttachmentAction.Delete) { storage.deleteAttachment(target.collection, target.id, target.imageSlot) }
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
        val result = AttachmentResult(message)
        results[target] = result
        if (target.imageSlot != null) imageResults[target.copy(imageSlot = null)] = result
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

internal fun decodePreview(bytes: ByteArray, maxSide: Int = 1600): Bitmap {
    if (Build.VERSION.SDK_INT >= 28) {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > maxSide) {
                decoder.setTargetSize(
                    (info.size.width.toLong() * maxSide / longest).coerceAtLeast(1).toInt(),
                    (info.size.height.toLong() * maxSide / longest).coerceAtLeast(1).toInt(),
                )
            }
        }
    }
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    val longest = maxOf(options.outWidth, options.outHeight)
    require(longest > 0)
    var sample = 1
    while ((longest.toLong() + sample - 1) / sample > maxSide) sample *= 2
    options.inSampleSize = sample
    options.inJustDecodeBounds = false
    val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options))
    if (options.outMimeType != "image/jpeg") return bitmap
    val orientation = try {
        ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (_: java.io.IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        .also { if (it !== bitmap) bitmap.recycle() }
}
