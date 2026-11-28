package com.jecsdev.gestiora.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf

enum class ClinicalAttachmentKind { Consultation, Exam, Journal }

typealias ClinicalAttachmentContent = @Composable (ClinicalAttachmentKind, String, Boolean, Boolean) -> Unit

internal val LocalClinicalAttachment = staticCompositionLocalOf<ClinicalAttachmentContent?> { null }

@Composable
internal fun ClinicalAttachmentActions(
    kind: ClinicalAttachmentKind,
    recordId: String,
    enabled: Boolean,
    showImages: Boolean = false,
) {
    key(kind, recordId) { LocalClinicalAttachment.current?.invoke(kind, recordId, enabled, showImages) }
}
