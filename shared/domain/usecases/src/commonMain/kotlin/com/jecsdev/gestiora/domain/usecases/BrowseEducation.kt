package com.jecsdev.gestiora.domain.usecases

import com.jecsdev.gestiora.domain.models.EducationalArticle
import com.jecsdev.gestiora.domain.repository.EducationalRepository

class BrowseEducation(private val repository: EducationalRepository) {
    suspend fun prepare(locale: String, version: Int, articles: List<EducationalArticle>) = repository.prepare(locale, version, articles)
    suspend fun search(locale: String, query: String, favoritesOnly: Boolean) = repository.search(locale, query, favoritesOnly)
    suspend fun setFavorite(id: String, favorite: Boolean) = repository.setFavorite(id, favorite)
}
