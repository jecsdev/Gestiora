package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*
import org.jetbrains.compose.resources.StringResource

/** Editorial summaries, not generated answers or personal medical assessments. */
internal data class EducationalSource(val institution: StringResource, val title: StringResource, val url: String, val language: StringResource)
internal data class BabyQuestion(
    val id: String,
    val question: StringResource,
    val answer: StringResource,
    val source: EducationalSource,
    val weeks: IntRange = 4..40,
)

internal const val BABY_QUESTIONS_CHECKED_ON = "2026-11-16"
internal const val BABY_QUESTIONS_VERSION = 1

private val whoPrenatal = EducationalSource(
    Res.string.who, Res.string.prenatal_care_who_recommendations,
    "https://www.who.int/news-room/detail/07-11-2016-pregnant-women-must-be-able-to-access-the-right-care-at-the-right-time-says-who", Res.string.english,
)
private val nhsMovements = EducationalSource(
    Res.string.nhs, Res.string.source_movements_title,
    "https://www.nhs.uk/pregnancy/keeping-well/your-babys-movements/", Res.string.english,
)
private fun unicefGuide(stage: String, title: StringResource) = EducationalSource(
    Res.string.unicef, title, "https://www.unicef.org/parenting/pregnancy-milestones/$stage-trimester", Res.string.english,
)

internal val babyQuestions = listOf(
    BabyQuestion("early-development", Res.string.what_begins_to_develop_in_the_early_weeks,
        Res.string.in_the_early_stage_the_brain_spinal_cord_full,
        unicefGuide("first", Res.string.source_first_title), 4..13),
    BabyQuestion("middle-development", Res.string.what_changes_occur_in_the_middle_stage,
        Res.string.bones_begin_to_harden_and_hearing_continues_to_full,
        unicefGuide("second", Res.string.source_second_title), 14..27),
    BabyQuestion("late-development", Res.string.what_keeps_developing_in_the_final_stage,
        Res.string.the_baby_continues_to_accumulate_fat_and_develop_full,
        unicefGuide("third", Res.string.source_third_title), 28..40),
    BabyQuestion("prenatal-contacts", Res.string.how_many_prenatal_contacts_are_recommended,
        Res.string.who_recommends_at_least_eight_contacts_with_health_full, whoPrenatal),
    BabyQuestion("early-ultrasound", Res.string.what_is_an_ultrasound_before_24_weeks_used,
        Res.string.who_recommends_an_ultrasound_before_24_weeks_to_full, whoPrenatal),
    BabyQuestion("first-movements", Res.string.when_are_movements_usually_first_felt,
        Res.string.they_are_usually_felt_between_weeks_16_and_full, nhsMovements),
    // Always available: a selected explorer week must never hide movement safety information.
    BabyQuestion("changed-movements", Res.string.what_should_i_do_if_the_pattern_of,
        Res.string.if_you_were_already_feeling_movements_and_now_full, nhsMovements),
)

/** Week ranges select related reading only; they are not diagnostic milestone boundaries. */
internal fun babyQuestionsFor(week: Int): List<BabyQuestion> =
    if (week !in 4..40) emptyList() else babyQuestions.filter { week in it.weeks }
