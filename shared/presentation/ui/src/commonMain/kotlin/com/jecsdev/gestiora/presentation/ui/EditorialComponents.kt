package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import gestiora.shared.presentation.ui.generated.resources.Res
import gestiora.shared.presentation.ui.generated.resources.back

@Composable
internal fun ScreenBackButton(onBack: () -> Unit) {
    val ui = LocalUiCopy.current
    TextButton(onClick = onBack) {
        val arrowColor = LocalContentColor.current
        Canvas(Modifier.size(20.dp)) {
            val middle = size.height / 2
            val tip = size.width * 0.2f
            val wing = size.width * 0.5f
            val arrow = Path().apply {
                moveTo(wing, size.height * 0.2f)
                lineTo(tip, middle)
                lineTo(wing, size.height * 0.8f)
            }
            val strokeWidth = 2.dp.toPx()
            drawPath(arrow, arrowColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(arrowColor, Offset(tip, middle), Offset(size.width * 0.85f, middle), strokeWidth, StrokeCap.Round)
        }
        Spacer(Modifier.width(8.dp))
        Text(ui.s(Res.string.back))
    }
}

@Composable
internal fun EditorialHeader(title: String, supporting: String? = null, eyebrow: String? = null, compact: Boolean = false) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        eyebrow?.let { Text(it, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
        Text(title, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() })
        supporting?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** Read-only container: actions remain explicit buttons inside the card. */
@Composable
internal fun EditorialRecordCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
internal fun EditorialStatus(label: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(50)) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

/** Shared by the editorial Home sections; the host supplies their real data and actions. */
@Composable
internal fun EditorialSection(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val fontScale = LocalDensity.current.fontScale
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val heading: @Composable () -> Unit = {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            val link: @Composable () -> Unit = {
                action?.let { TextButton(onClick = onAction) { Text(it) } }
            }
            if (maxWidth < 300.dp || fontScale > 1.2f) {
                Column { heading(); link() }
            } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { heading() }
                link()
            }
        }
        content()
    }
}

@Composable
internal fun EditorialCard(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        content = content,
    )
}

@Composable
internal fun EditorialFollowUpRow(title: String, supporting: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Column(Modifier.heightIn(min = 56.dp).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
}
