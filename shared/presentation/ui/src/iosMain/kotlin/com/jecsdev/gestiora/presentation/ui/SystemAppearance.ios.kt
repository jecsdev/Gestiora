package com.jecsdev.gestiora.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.interop.LocalUIViewController
import platform.UIKit.UIUserInterfaceStyle

@Composable
internal actual fun SystemAppearance(dark: Boolean, followSystem: Boolean) {
    val controller = LocalUIViewController.current
    SideEffect {
        controller.overrideUserInterfaceStyle = when {
            followSystem -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            dark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
            else -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
        }
    }
}
