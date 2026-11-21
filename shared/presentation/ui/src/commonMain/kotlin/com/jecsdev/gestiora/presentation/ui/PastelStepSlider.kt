package com.jecsdev.gestiora.presentation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun PastelStepSlider(
    value: Int,
    range: IntRange,
    label: String,
    valueLabel: String,
    onValueChange: (Int) -> Unit,
) {
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt().coerceIn(range)) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = range.last - range.first - 1,
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = label
            stateDescription = valueLabel
        },
        track = { state -> PastelStepSliderTrack(state.coercedValueAsFraction) },
    )
}

@Composable
private fun PastelStepSliderTrack(progress: Float) {
    val gradient = accentGradient()
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    val reducedMotion = LocalReducedMotion.current
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 120),
        label = "Slider track",
    )
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(color = inactiveColor, cornerRadius = radius)
        val activeWidth = size.width * animatedProgress.value
        if (activeWidth > 0f) {
            val direction = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
            scale(scaleX = direction, scaleY = 1f) {
                drawRoundRect(
                    brush = gradient,
                    size = Size(activeWidth, size.height),
                    cornerRadius = radius,
                )
            }
        }
    }
}
