package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource

/** The closest earlier bundled illustration is an educational reference, not an ultrasound. */
@Composable
internal fun BabyIllustration(week: Int, modifier: Modifier = Modifier) {
    val ui = LocalUiCopy.current
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
    Image(painterResource(image), contentDescription = ui.s(Res.string.illustrative_fetal_image_for_the_stage_around_week, referenceWeek), modifier = modifier)
}
