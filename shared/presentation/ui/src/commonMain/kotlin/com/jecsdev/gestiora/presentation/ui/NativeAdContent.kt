package com.jecsdev.gestiora.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class NativeAdPlacement { HomeFooter, SettingsFooter }

/** Render only when a creative is available. The host supplies placement and no profile data. */
typealias NativeAdContent = @Composable (NativeAdPlacement) -> Unit

internal val LocalNativeAd = staticCompositionLocalOf<NativeAdContent?> { null }
