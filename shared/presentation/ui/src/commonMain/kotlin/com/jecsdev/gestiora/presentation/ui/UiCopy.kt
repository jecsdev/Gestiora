package com.jecsdev.gestiora.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import gestiora.shared.presentation.ui.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Resolved resource strings also serve event callbacks and pure presentation formatters.
 * Never used to translate or rewrite user-entered or persisted medical data.
 */
internal class UiCopy(private val strings: Map<StringResource, String>) {
    fun s(resource: StringResource, vararg args: Any?): String {
        val text = strings.getValue(resource)
        // Replace tokens in one pass: a user's own text must never become a format directive.
        return Regex("%([0-9]+)[$]s|%%").replace(text) { match ->
            if (match.value == "%%") "%" else args[match.groupValues[1].toInt() - 1]?.toString().orEmpty()
        }
    }
}

internal val LocalUiCopy = staticCompositionLocalOf<UiCopy> { error("UiCopyProvider is required") }

@Composable
internal fun UiCopyProvider(content: @Composable () -> Unit) {
    // Compose resolves qualifiers from the current platform resource environment.
    // Read once at the root, so callbacks use the same language as the rendered UI.
    val strings = Res.allStringResources.values.associateWith { stringResource(it) }
    val copy = remember(strings) { UiCopy(strings) }
    CompositionLocalProvider(LocalUiCopy provides copy, content = content)
}
