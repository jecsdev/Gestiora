package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jecsdev.gestiora.core.ui.AppTheme
import com.jecsdev.gestiora.domain.models.DatingMethod
import com.jecsdev.gestiora.domain.models.PregnancyDating
import com.jecsdev.gestiora.domain.models.PregnancyProfile

internal val previewProfile = PregnancyProfile(
    motherName = "Jane Doe",
    babyName = "Mike",
    country = "República Dominicana",
    language = "es",
    dating = PregnancyDating(method = DatingMethod.FUM, lmp = "2026-06-27", dueDate = "2027-04-03"),
)

@Composable
internal fun PreviewContent(dark: Boolean = false, content: @Composable () -> Unit) {
    UiCopyProvider {
        AppTheme(darkTheme = dark) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
        }
    }
}
