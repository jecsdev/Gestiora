package com.jecsdev.gestiora.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val LocalReducedMotion = staticCompositionLocalOf { false }
internal val LocalScreenLeaving = staticCompositionLocalOf { false }

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

/** Switch after the exit completes so only one screen owns back handling and editable state. */
@Composable
internal fun <T> ScreenExchange(
    target: T,
    animate: (T, T) -> Boolean = { _, _ -> true },
    content: @Composable (T) -> Unit,
) {
    val reduced = LocalReducedMotion.current
    var displayed by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    val distance = with(LocalDensity.current) { 12.dp.toPx() }
    val leaving = !reduced && displayed != target && animate(displayed, target)
    var entering by remember { mutableStateOf(false) }

    LaunchedEffect(target, reduced) {
        if (!reduced && displayed != target && animate(displayed, target)) {
            entering = false
            progress.animateTo(0f, tween(115))
            displayed = target
            entering = true
            progress.animateTo(1f, tween(180))
        } else {
            displayed = target
            progress.snapTo(1f)
        }
    }

    val screen = if (leaving) displayed else target
    val blocked = LocalScreenLeaving.current || leaving
    Box(Modifier.fillMaxSize()) {
        AppointmentBackHandler(enabled = leaving) {}
        CompositionLocalProvider(LocalScreenLeaving provides blocked) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        val visible = if (reduced) 1f else progress.value
                        alpha = visible
                        translationX = (1f - visible) * if (entering) distance else -distance
                    }
                    .then(if (blocked) Modifier.semantics { hideFromAccessibility() } else Modifier),
            ) { content(screen) }
        }
        if (leaving) {
            Box(Modifier.matchParentSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            })
        }
    }
}
