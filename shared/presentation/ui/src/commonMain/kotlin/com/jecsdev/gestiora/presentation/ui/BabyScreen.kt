package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*
import org.jetbrains.compose.resources.StringResource

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import com.jecsdev.gestiora.presentation.viewmodels.ProfileViewModel
import com.jecsdev.gestiora.presentation.viewmodels.EducationViewModel
import org.koin.compose.koinInject
import kotlin.math.roundToInt

internal data class BabyStage(val from: Int, val size: StringResource, val text: StringResource, val milestones: List<StringResource>)

private val stages = listOf(
    BabyStage(0, Res.string.a_poppy_seed, Res.string.early_structures_form_and_neural_tube_development_begins, listOf(Res.string.implantation, Res.string.neural_tube_formation_begins, Res.string.the_primitive_heart_forms)),
    BabyStage(8, Res.string.a_raspberry, Res.string.fingers_facial_features_and_major_organs_develop, listOf(Res.string.developing_fingers, Res.string.spontaneous_movements, Res.string.developing_organs)),
    BabyStage(12, Res.string.a_lime, Res.string.the_main_structures_continue_to_develop, listOf(Res.string.early_reflexes, Res.string.developing_nails, Res.string.end_of_the_first_trimester)),
    BabyStage(16, Res.string.an_avocado, Res.string.muscles_strengthen_and_the_baby_may_respond_to, listOf(Res.string.coordinated_movements, Res.string.hardening_skeleton, Res.string.developing_hearing)),
    BabyStage(20, Res.string.a_banana, Res.string.many_people_begin_to_notice_movements_more_clearly, listOf(Res.string.sleep_cycles, Res.string.protective_vernix, Res.string.noticeable_movements)),
    BabyStage(24, Res.string.an_ear_of_corn, Res.string.the_lungs_and_brain_continue_to_mature_rapidly, listOf(Res.string.responds_to_sounds, Res.string.developing_lungs, Res.string.gaining_body_fat)),
    BabyStage(28, Res.string.an_eggplant, Res.string.the_baby_can_open_and_close_their_eyes, listOf(Res.string.opens_eyes, Res.string.brain_growth, Res.string.start_of_the_third_trimester)),
    BabyStage(32, Res.string.a_small_pumpkin, Res.string.the_baby_accumulates_fat_and_practises_breathing_movements, listOf(Res.string.complete_nails, Res.string.firmer_bones, Res.string.changing_position)),
    BabyStage(36, Res.string.a_romaine_lettuce, Res.string.the_baby_continues_to_gain_weight_and_prepare, listOf(Res.string.lung_maturation, Res.string.movements_continue_until_birth, Res.string.final_preparations)),
)

internal fun babyStage(week: Int): BabyStage = stages.last { week.coerceIn(4, 40) >= it.from }
internal fun babyReferenceWeek(week: Int): Int = listOf(36, 28, 20, 16, 12, 8, 4).first { week.coerceIn(4, 40) >= it }

@Composable
fun BabyScreen(profile: PregnancyProfile, viewModel: ProfileViewModel) {
    val ui = LocalUiCopy.current
    var catalogOpen by retain { mutableStateOf(false) }
    val actualWeek = viewModel.preview(profile.dating, todayIsoDate())?.weeks
    val nearestWeek = actualWeek?.coerceIn(4, 40)
    var chosen by retain(profile.dating) { mutableStateOf<Int?>(null) }
    val week = chosen ?: nearestWeek ?: 4
    val stage = babyStage(week)
    val referenceWeek = babyReferenceWeek(week)
    val scroll = rememberScrollState()
    if (catalogOpen) {
        EducationalCatalogScreen(koinInject<EducationViewModel>()) { catalogOpen = false }
        return
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding()
            .verticalScroll(scroll).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        EditorialHeader(profile.babyName?.takeIf { it.isNotBlank() } ?: ui.s(Res.string.my_baby), eyebrow = ui.s(Res.string.each_week_a_little_miracle))
        if (actualWeek == null) Text(ui.s(Res.string.check_your_profile_dates_to_find_out_your), style = MaterialTheme.typography.bodySmall)
        if (actualWeek != null && actualWeek !in 4..40) Text(ui.s(Res.string.your_current_week_is_the_explorer_covers_weeks, actualWeek), style = MaterialTheme.typography.bodySmall)
        PastelHero {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    WeekNavigationButton(
                        onClick = { chosen = week - 1 },
                        enabled = week > 4,
                        description = ui.s(Res.string.previous_week),
                        pointsRight = false,
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ui.s(Res.string.explore_development), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        Text(ui.s(Res.string.week, week), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    }
                    WeekNavigationButton(
                        onClick = { chosen = week + 1 },
                        enabled = week < 40,
                        description = ui.s(Res.string.next_week),
                        pointsRight = true,
                    )
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(240.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.32f), CircleShape))
                    BabyIllustration(week, Modifier.fillMaxWidth().heightIn(max = 280.dp))
                }
                Text(ui.s(Res.string.illustrative_reference_stage_week_does_not_represent_exact, referenceWeek), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                Slider(value = week.toFloat(), onValueChange = { chosen = it.roundToInt().coerceIn(4, 40) }, valueRange = 4f..40f, steps = 35, modifier = Modifier.semantics { contentDescription = ui.s(Res.string.pregnancy_week) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(ui.s(Res.string.week_4), style = MaterialTheme.typography.labelSmall)
                    Text(ui.s(Res.string.week_40), style = MaterialTheme.typography.labelSmall)
                }
                if (nearestWeek != null && week != nearestWeek) TextButton(onClick = { chosen = null }) {
                    Text(if (actualWeek == nearestWeek) ui.s(Res.string.back_to_my_week, actualWeek) else ui.s(Res.string.back_to_week, nearestWeek))
                }
            }
        }
        EditorialRecordCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui.s(Res.string.approximate_development), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(ui.s(Res.string.about_the_size_of, ui.s(stage.size)), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            }
        }
        EditorialSection(ui.s(Res.string.small_big_milestones)) {
            Text(ui.s(stage.text), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            stage.milestones.forEachIndexed { index, milestone ->
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("0${index + 1}", color = MaterialTheme.colorScheme.primary)
                    Text(ui.s(milestone), style = MaterialTheme.typography.bodyMedium)
                }
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            }
        }
        OutlinedButton(onClick = { catalogOpen = true }, modifier = Modifier.fillMaxWidth()) {
            Text(ui.s(Res.string.catalog_open))
        }
        BabyQuestionsSection(week)
        Text(ui.s(Res.string.every_pregnancy_is_different_this_information_is_educational), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeekNavigationButton(
    onClick: () -> Unit,
    enabled: Boolean,
    description: String,
    pointsRight: Boolean,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 56.dp, height = 48.dp)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp),
    ) {
        val color = LocalContentColor.current
        Canvas(Modifier.size(24.dp)) {
            val direction = if (pointsRight) 1 else -1
            val tip = center + Offset(3.dp.toPx() * direction, 0f)
            val tailX = center.x - 3.dp.toPx() * direction
            val halfHeight = 6.dp.toPx()
            val chevron = Path().apply {
                moveTo(tailX, center.y - halfHeight)
                lineTo(tip.x, tip.y)
                lineTo(tailX, center.y + halfHeight)
            }
            drawPath(chevron, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
