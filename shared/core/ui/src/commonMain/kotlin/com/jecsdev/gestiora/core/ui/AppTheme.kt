package com.jecsdev.gestiora.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import gestiora.shared.core.ui.generated.resources.*
import org.jetbrains.compose.resources.Font

private val lightColors = lightColorScheme(
    primary = Color(0xFF7B47A3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE0FF),
    onPrimaryContainer = Color(0xFF392343),
    secondary = Color(0xFFA64C70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4EC),
    onSecondaryContainer = Color(0xFF58253C),
    tertiary = Color(0xFF945044),
    tertiaryContainer = Color(0xFFFFE3DE),
    onTertiaryContainer = Color(0xFF5A2923),
    background = Color(0xFFFBF7F1),
    onBackground = Color(0xFF302836),
    surface = Color(0xFFFFFDFC),
    onSurface = Color(0xFF302836),
    surfaceVariant = Color(0xFFF5EDF5),
    onSurfaceVariant = Color(0xFF6D6470),
    outline = Color(0xFF89758F),
    outlineVariant = Color(0xFFE5DCEA),
)

private val darkColors = darkColorScheme(
    primary = Color(0xFFD0ACEC),
    onPrimary = Color(0xFF30203C),
    primaryContainer = Color(0xFF453052),
    onPrimaryContainer = Color(0xFFF3DDFB),
    secondary = Color(0xFFE2A8B9),
    secondaryContainer = Color(0xFF533844),
    onSecondaryContainer = Color(0xFFFFDFE9),
    tertiary = Color(0xFFF0B5AA),
    tertiaryContainer = Color(0xFF573833),
    onTertiaryContainer = Color(0xFFFFE1DB),
    background = Color(0xFF211B25),
    onBackground = Color(0xFFF5EEF4),
    surface = Color(0xFF2C2530),
    onSurface = Color(0xFFF5EEF4),
    surfaceVariant = Color(0xFF372B3C),
    onSurfaceVariant = Color(0xFFCEBFCD),
    outline = Color(0xFFA28CAB),
    outlineVariant = Color(0xFF514456),
)

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
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
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        typography = typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(20.dp),
            small = RoundedCornerShape(20.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
    ) {
        // MaterialTheme supplies the palette; Surface supplies the default content color.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            if (darkTheme) listOf(Color(0xFF302437), Color(0xFF2A202B), darkColors.background, darkColors.background)
            else listOf(Color(0xFFF3EBFA), Color(0xFFFFF0EC), lightColors.background, lightColors.background),
        ))) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content,
        )
        }
    }
}
