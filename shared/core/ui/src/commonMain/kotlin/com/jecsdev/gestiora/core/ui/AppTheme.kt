package com.jecsdev.gestiora.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import gestiora.shared.core.ui.generated.resources.*
import org.jetbrains.compose.resources.Font

private val lightColors = lightColorScheme(
    primary = Color(0xFF80559E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEDFF5),
    onPrimaryContainer = Color(0xFF392343),
    secondary = Color(0xFFB76882),
    secondaryContainer = Color(0xFFF8E5EB),
    background = Color(0xFFFCF8F4),
    onBackground = Color(0xFF302836),
    surface = Color(0xFFFFFEFC),
    onSurface = Color(0xFF302836),
    surfaceVariant = Color(0xFFF5EDF5),
    onSurfaceVariant = Color(0xFF6D6470),
)

private val darkColors = darkColorScheme(
    primary = Color(0xFFCAA5DE),
    onPrimary = Color(0xFF30203C),
    primaryContainer = Color(0xFF47304F),
    onPrimaryContainer = Color(0xFFF3DDFB),
    secondary = Color(0xFFE2A8B9),
    secondaryContainer = Color(0xFF533844),
    background = Color(0xFF211B24),
    onBackground = Color(0xFFF5EEF4),
    surface = Color(0xFF2C2530),
    onSurface = Color(0xFFF5EEF4),
    surfaceVariant = Color(0xFF372B3C),
    onSurfaceVariant = Color(0xFFCEBFCD),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val display = FontFamily(
        Font(Res.font.plus_jakarta_sans_400, FontWeight.Normal),
        Font(Res.font.plus_jakarta_sans_500, FontWeight.Medium),
        Font(Res.font.plus_jakarta_sans_600, FontWeight.SemiBold),
        Font(Res.font.plus_jakarta_sans_700, FontWeight.Bold),
        Font(Res.font.plus_jakarta_sans_800, FontWeight.ExtraBold),
    )
    val body = FontFamily(
        Font(Res.font.dm_sans_400, FontWeight.Normal),
        Font(Res.font.dm_sans_500, FontWeight.Medium),
        Font(Res.font.dm_sans_600, FontWeight.SemiBold),
        Font(Res.font.dm_sans_700, FontWeight.Bold),
    )
    val defaults = Typography()
    val typography = Typography(
        displayLarge = defaults.displayLarge.copy(fontFamily = display),
        displayMedium = defaults.displayMedium.copy(fontFamily = display),
        displaySmall = defaults.displaySmall.copy(fontFamily = display),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = display),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = display),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = display),
        titleLarge = defaults.titleLarge.copy(fontFamily = display),
        titleMedium = defaults.titleMedium.copy(fontFamily = display),
        titleSmall = defaults.titleSmall.copy(fontFamily = display),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = body),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = body),
        bodySmall = defaults.bodySmall.copy(fontFamily = body),
        labelLarge = defaults.labelLarge.copy(fontFamily = body),
        labelMedium = defaults.labelMedium.copy(fontFamily = body),
        labelSmall = defaults.labelSmall.copy(fontFamily = body),
    )
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColors else lightColors, typography = typography, content = content)
}
