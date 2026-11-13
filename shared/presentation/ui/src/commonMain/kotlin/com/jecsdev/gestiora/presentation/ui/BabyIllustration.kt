package com.jecsdev.gestiora.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import gestiora.shared.presentation.ui.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/** The closest earlier bundled illustration is an educational reference, not an ultrasound. */
@Composable
internal fun BabyIllustration(week: Int, modifier: Modifier = Modifier) {
    val referenceWeek = babyReferenceWeek(week)
    val image = when (referenceWeek) {
        4 -> Res.drawable.embryo_week_04
        8 -> Res.drawable.embryo_week_08
        12 -> Res.drawable.fetus_week_12
        16 -> Res.drawable.fetus_week_16
        20 -> Res.drawable.fetus_week_20
        28 -> Res.drawable.fetus_week_28
        else -> Res.drawable.fetus_week_36
    }
    Image(painterResource(image), contentDescription = "Ilustración fetal orientativa de la etapa de la semana $referenceWeek, no una ecografía", modifier = modifier)
}
