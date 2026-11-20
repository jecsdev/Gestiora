package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gestiora.shared.presentation.ui.generated.resources.Res
import gestiora.shared.presentation.ui.generated.resources.fetus_week_36
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun WelcomeScreen(onStart: () -> Unit, restoreBackup: (@Composable () -> Unit)? = null) {
    val ui = LocalUiCopy.current
    BoxWithConstraints(
        Modifier.fillMaxSize().safeDrawingPadding(),
    ) {
        val viewportHeight = maxHeight
        // Like the reference's min-h-dvh/flex layout, fill tall screens but allow overflow
        // on short windows or with large fonts. No fixed screen height or clipped actions.
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = viewportHeight),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                ui.s(Res.string.brand),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
            )
            Box(
                Modifier.fillMaxWidth().height((viewportHeight * 0.36f).coerceAtLeast(240.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(Res.drawable.fetus_week_36),
                    contentDescription = ui.s(Res.string.reference_fetal_illustration),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            Column(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f), RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            ) {
                Text(ui.s(Res.string.your_prenatal_space), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(
                    buildAnnotatedString {
                        append(ui.s(Res.string.your_pregnancy_n))
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontStyle = FontStyle.Italic)) { append(ui.s(Res.string.at_ease)) }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium),
                )
                Text(
                    ui.s(Res.string.appointments_records_and_every_little_milestone_together_in),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GradientButton(onClick = onStart, modifier = Modifier.padding(top = 24.dp).fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(ui.s(Res.string.get_started))
                }
                restoreBackup?.invoke()
                Text(
                    ui.s(Res.string.you_can_start_without_creating_an_account),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    ui.s(Res.string.catalog_disclaimer),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
