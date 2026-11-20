package com.jecsdev.gestiora.data.datasources

import com.jecsdev.gestiora.data.models.EducationalContentData

interface EducationalStore {
    suspend fun seed(locale: String, version: Int, articles: List<EducationalContentData>)
    suspend fun search(locale: String, normalizedQuery: String, favoritesOnly: Boolean): List<EducationalContentData>
    suspend fun setFavorite(id: String, favorite: Boolean)
}
