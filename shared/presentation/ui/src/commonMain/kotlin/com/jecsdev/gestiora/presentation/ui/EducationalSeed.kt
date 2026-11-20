package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.ArticleSource
import com.jecsdev.gestiora.domain.models.EducationalArticle
import gestiora.shared.presentation.ui.generated.resources.*

/** Resource adapter: materialize only the resolved locale; no duplicate translated medical copy. */
internal fun educationalSeed(ui: UiCopy): List<EducationalArticle> = babyQuestions.map { entry ->
    EducationalArticle(
        id = entry.id,
        locale = ui.s(Res.string.catalog_locale),
        title = ui.s(entry.question),
        body = ui.s(entry.answer),
        tags = ui.s(when (entry.id) {
            "early-development", "middle-development", "late-development" -> Res.string.catalog_tags_development
            "prenatal-contacts", "early-ultrasound" -> Res.string.catalog_tags_prenatal
            else -> Res.string.catalog_tags_movements
        }),
        sources = listOf(ArticleSource(ui.s(entry.source.institution), ui.s(entry.source.title), entry.source.url, "en")),
        // Version of the existing bundled editorial content, not a new source/medical review.
        updatedAt = 1794787200000L,
        sourceCheckedOn = BABY_QUESTIONS_CHECKED_ON,
    )
}
