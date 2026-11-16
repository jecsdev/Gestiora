package com.jecsdev.gestiora.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
internal expect fun rememberReducedMotion(): Boolean

@Composable
internal fun MotionPreferences(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalReducedMotion provides rememberReducedMotion(), content = content)
}

/** Entry-only motion keeps a single active destination, its callbacks and retained drafts.
 * It never keeps a hidden outgoing screen interactive during a crossfade.
 */
@Composable
internal fun Modifier.screenEntrance(distance: Dp = 28.dp): Modifier {
    val reduced = LocalReducedMotion.current
    var entered by retain { mutableStateOf(false) }
    val progress = remember { Animatable(if (entered || reduced) 1f else 0f) }
    val pixels = with(LocalDensity.current) { distance.toPx() }
    LaunchedEffect(reduced) {
        entered = true
        if (reduced) progress.snapTo(1f) else progress.animateTo(1f, tween(220))
    }
    return graphicsLayer {
        alpha = progress.value
        translationX = (1f - progress.value) * pixels
    }
}
