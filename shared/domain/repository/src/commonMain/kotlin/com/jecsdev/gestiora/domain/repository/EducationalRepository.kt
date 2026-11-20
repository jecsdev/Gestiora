package com.jecsdev.gestiora.domain.repository

import com.jecsdev.gestiora.domain.models.EducationalArticle

interface EducationalRepository {
    suspend fun prepare(locale: String, version: Int, articles: List<EducationalArticle>)
    suspend fun search(locale: String, query: String, favoritesOnly: Boolean): List<EducationalArticle>
    suspend fun setFavorite(id: String, favorite: Boolean)
}
