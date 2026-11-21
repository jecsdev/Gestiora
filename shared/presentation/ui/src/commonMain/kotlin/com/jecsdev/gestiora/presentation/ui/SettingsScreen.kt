package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.ThemeMode
import gestiora.shared.presentation.ui.generated.resources.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun SettingsScreen(
    preferences: @Composable () -> Unit,
    exportBackup: (@Composable () -> Unit)?,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    val ui = LocalUiCopy.current
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenBackButton(onBack)
            EditorialHeader(ui.s(Res.string.settings_title))
        }
        EditorialSection(ui.s(Res.string.settings_data_and_privacy)) {
            EditorialRecordCard {
                Text(ui.s(Res.string.settings_local_storage), style = MaterialTheme.typography.bodyMedium)
                if (exportBackup != null) {
                    exportBackup()
                    Text(ui.s(Res.string.settings_restore_onboarding), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        preferences()
        EditorialSection(ui.s(Res.string.settings_premium_title)) {
            EditorialRecordCard {
                Text(ui.s(Res.string.settings_premium_preview), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onPremium) { Text(ui.s(Res.string.premium_view_benefits)) }
            }
        }
        LocalNativeAd.current?.invoke(NativeAdPlacement.SettingsFooter)
    }
}

@Preview(name = "Settings · dark", widthDp = 384, heightDp = 780, locale = "en")
@Composable
private fun SettingsScreenPreview() {
    PreviewContent(dark = true) {
        SettingsScreen(
            preferences = {
                ThemePreferences(ThemeMode.DARK, busy = false, readFailed = false, saveFailed = false, onRetry = {}, onSelect = {})
            },
            exportBackup = {
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Export encrypted backup") }
            },
            onBack = {},
            onPremium = {},
        )
    }
}

@Composable
internal fun SettingsIcon() {
    val color = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val toothCount = 8
        val toothAngle = 2 * PI / toothCount
        val radius = size.minDimension / 2
        val outline = Path()
        val toothEdge = listOf(-0.34 to 0.67f, -0.22 to 0.86f, 0.22 to 0.86f, 0.34 to 0.67f)
        repeat(toothCount) { tooth ->
            toothEdge.forEachIndexed { index, (fraction, depth) ->
                val angle = (tooth + fraction) * toothAngle
                val point = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (radius * depth)
                if (tooth == 0 && index == 0) outline.moveTo(point.x, point.y) else outline.lineTo(point.x, point.y)
            }
        }
        outline.close()
        drawPath(outline, color, style = Stroke(1.6.dp.toPx(), join = StrokeJoin.Round))
        drawCircle(color, radius * 0.28f, style = Stroke(1.6.dp.toPx()))
    }
}
