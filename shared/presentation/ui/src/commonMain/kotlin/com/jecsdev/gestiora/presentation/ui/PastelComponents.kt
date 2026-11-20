package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** Preserve Material editing, validation and accessibility while sharing the form surface. */
@Composable
internal fun PastelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(value, onValueChange, modifier = modifier, enabled = enabled, label = label,
        singleLine = singleLine, isError = isError, supportingText = supportingText,
        keyboardOptions = keyboardOptions, trailingIcon = trailingIcon, shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surface, unfocusedContainerColor = colors.surface,
            disabledContainerColor = colors.surfaceVariant, errorContainerColor = colors.surface,
            unfocusedBorderColor = colors.outlineVariant,
        ))
}

@Composable
internal fun accentGradient(): Brush {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.2f
    return Brush.horizontalGradient(if (dark) listOf(Color(0xFFCAA4E6), Color(0xFFE6A3BA))
        else listOf(Color(0xFF7B47A3), Color(0xFFA74870)))
}

@Composable
internal fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val brush = if (enabled) accentGradient() else Brush.horizontalGradient(listOf(colors.surfaceVariant, colors.surfaceVariant))
    Button(onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp).background(brush, CircleShape),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = colors.onPrimary,
            disabledContainerColor = Color.Transparent, disabledContentColor = colors.onSurfaceVariant),
        content = content)
}

/** Decorative gradients stay behind opaque, theme-aware text; no new clinical semantics. */
@Composable
internal fun PastelHero(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp),
        color = Color.Transparent, contentColor = colors.onPrimaryContainer, shadowElevation = 3.dp) {
        Column(Modifier.background(Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer)))
            .drawBehind {
                drawCircle(colors.surface.copy(alpha = 0.24f), radius = size.width * 0.35f,
                    center = Offset(size.width, size.height * 0.08f))
            }.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
internal fun EditorialEmptyState(message: String) {
    EditorialRecordCard {
        Column(Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Box(Modifier.padding(14.dp)) { DestinationIcon(AppDestination.Records) }
            }
            Text(message, style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
