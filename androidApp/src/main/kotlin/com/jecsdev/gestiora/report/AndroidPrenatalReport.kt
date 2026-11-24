package com.jecsdev.gestiora.report

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
import com.jecsdev.gestiora.data.datasources.core.room.AttachmentCollection
import com.jecsdev.gestiora.data.datasources.core.room.ProtectedClinicalStorage
import com.jecsdev.gestiora.presentation.ui.ClinicalAttachmentKind
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
        attachmentName = export::attachmentName,
        preview = { document -> withContext(Dispatchers.Default) { previewPrenatalReport(document) } },
    )
}

private class ReportExport(private val app: GestioraApplication) : RetainObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pending: PrenatalReportDocument? = null
    var status by mutableStateOf(PrenatalReportExport.Idle)
        private set

    suspend fun attachmentName(kind: ClinicalAttachmentKind, id: String): String? = withContext(Dispatchers.IO) {
        val collection = when (kind) {
            ClinicalAttachmentKind.Consultation -> AttachmentCollection.Consultation
            ClinicalAttachmentKind.Exam -> AttachmentCollection.Exam
        }
        val storage = app.graph.koin.get<ProtectedClinicalStorage>()
        storage.readAttachment(collection, id)?.let { attachment ->
            try { attachment.name } finally { attachment.bytes.fill(0) }
        }
    }

    fun choose(document: PrenatalReportDocument, launch: (String) -> Unit) {
        if (status == PrenatalReportExport.Saving) return
        pending = document
        status = PrenatalReportExport.Saving
        try {
            launch("Gestiora-${document.month}.pdf")
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
                        writePrenatalPdf(document, output)
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
