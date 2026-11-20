package com.jecsdev.gestiora.domain.models

data class ArticleSource(val institution: String, val title: String, val url: String, val language: String)

/** Public editorial content. Dates do not imply medical approval. */
data class EducationalArticle(
    val id: String,
    val locale: String,
    val title: String,
    val body: String,
    val tags: String,
    val sources: List<ArticleSource>,
    val updatedAt: Long,
    val sourceCheckedOn: String,
    val reviewedOn: String? = null,
    val isFavorite: Boolean = false,
)
