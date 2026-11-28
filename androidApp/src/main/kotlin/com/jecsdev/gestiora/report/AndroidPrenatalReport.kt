package com.jecsdev.gestiora.report

import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import com.jecsdev.gestiora.GestioraApplication
import com.jecsdev.gestiora.attachments.decodePreview
import com.jecsdev.gestiora.data.datasources.core.room.AttachmentCollection
import com.jecsdev.gestiora.data.datasources.core.room.ProtectedClinicalStorage
import com.jecsdev.gestiora.presentation.ui.ClinicalAttachmentKind
import com.jecsdev.gestiora.presentation.ui.PrenatalReportAttachment
import com.jecsdev.gestiora.presentation.ui.PrenatalReportBlock
import com.jecsdev.gestiora.presentation.ui.PrenatalReportControls
import com.jecsdev.gestiora.presentation.ui.PrenatalReportDocument
import com.jecsdev.gestiora.presentation.ui.PrenatalReportExport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

@Composable
internal fun rememberPrenatalReport(app: GestioraApplication): PrenatalReportControls {
    val export = retain { ReportExport(app) }
    // Register above the shared UI's RAM-only saveable-state boundary.
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
        export::write,
    )
    return PrenatalReportControls(
        status = export.status,
        export = { document -> export.choose(document, createDocument::launch) },
        attachments = export::attachments,
        preview = { document ->
            withContext(Dispatchers.IO) {
                val maxSide = imageDecodeLimit(document, maxSide = 640, pixelBudget = 8_000_000)
                previewPrenatalReport(document) { export.loadImage(it, maxSide) }
            }
        },
    )
}

private class ReportExport(private val app: GestioraApplication) : RetainObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val storage get() = app.graph.koin.get<ProtectedClinicalStorage>()
    private var pending: PrenatalReportDocument? = null
    var status by mutableStateOf(PrenatalReportExport.Idle)
        private set

    suspend fun attachments(kind: ClinicalAttachmentKind, id: String, includeImages: Boolean): List<PrenatalReportAttachment> =
        withContext(Dispatchers.IO) {
            val collection = kind.collection()
            val slots = if (includeImages) storage.imageSlots(collection, id) else emptyList()
            (listOf(null) + slots).mapNotNull { slot ->
                storage.readAttachment(collection, id, slot)?.let { attachment ->
                    try {
                        PrenatalReportAttachment(kind, id, attachment.name, attachment.mimeType.startsWith("image/"), slot)
                    } finally {
                        attachment.bytes.fill(0)
                    }
                }
            }
        }

    suspend fun loadImage(image: PrenatalReportAttachment, maxSide: Int): Bitmap {
        val attachment = checkNotNull(storage.readAttachment(image.kind.collection(), image.recordId, image.imageSlot))
        return try {
            check(attachment.name == image.name && attachment.mimeType.startsWith("image/"))
            decodePreview(attachment.bytes, maxSide)
        } finally {
            attachment.bytes.fill(0)
        }
    }

    fun choose(document: PrenatalReportDocument, launch: (String) -> Unit) {
        if (status == PrenatalReportExport.Saving) return
        pending = document
        status = PrenatalReportExport.Saving
        try {
            launch(document.filename)
        } catch (_: RuntimeException) {
            pending = null
            status = PrenatalReportExport.Failed
        }
    }

    fun write(destination: Uri?) {
        val document = pending ?: return
        pending = null
        if (destination == null) {
            status = PrenatalReportExport.Idle
            return
        }
        scope.launch {
            status = withContext(Dispatchers.IO) {
                val resolver = app.contentResolver
                try {
                    checkNotNull(resolver.openOutputStream(destination, "wt")).use { output ->
                        val maxSide = imageDecodeLimit(document, maxSide = 1600, pixelBudget = 24_000_000)
                        writePrenatalPdf(document, output) { loadImage(it, maxSide) }
                    }
                    PrenatalReportExport.Saved
                } catch (failure: Exception) {
                    runCatching { DocumentsContract.deleteDocument(resolver, destination) }
                    if (failure is CancellationException) throw failure
                    PrenatalReportExport.Failed
                }
            }
        }
    }

    override fun onRetained() = Unit
    override fun onEnteredComposition() = Unit
    override fun onExitedComposition() = Unit
    override fun onUnused() = scope.cancel()
    override fun onRetired() {
        pending = null
        scope.cancel()
    }
}

// PDF pages and preview Pictures can retain their bitmaps until rendering finishes.
private fun imageDecodeLimit(document: PrenatalReportDocument, maxSide: Int, pixelBudget: Int): Int {
    val count = document.blocks.count { it is PrenatalReportBlock.Image }.coerceAtLeast(1)
    return minOf(maxSide, sqrt(pixelBudget.toDouble() / count).toInt())
}

private fun ClinicalAttachmentKind.collection(): AttachmentCollection = when (this) {
    ClinicalAttachmentKind.Consultation -> AttachmentCollection.Consultation
    ClinicalAttachmentKind.Exam -> AttachmentCollection.Exam
    ClinicalAttachmentKind.Journal -> error("Journal images are private")
}
