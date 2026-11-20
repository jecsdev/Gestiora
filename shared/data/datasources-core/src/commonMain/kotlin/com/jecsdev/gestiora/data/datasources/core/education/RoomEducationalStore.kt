package com.jecsdev.gestiora.data.datasources.core.education

import com.jecsdev.gestiora.data.datasources.EducationalStore
import com.jecsdev.gestiora.data.models.EducationalContentData
import com.jecsdev.gestiora.data.models.EducationalSourceData

class RoomEducationalStore(private val dao: EducationalDao) : EducationalStore {
    override suspend fun seed(locale: String, version: Int, articles: List<EducationalContentData>) = dao.seed(locale, version, articles)
    override suspend fun setFavorite(id: String, favorite: Boolean) = dao.saveFavorite(EducationalFavoriteRow(id, favorite))
    override suspend fun search(locale: String, normalizedQuery: String, favoritesOnly: Boolean) =
        dao.search(locale, normalizedQuery, favoritesOnly).map { result ->
            val row = result.content
            EducationalContentData(row.content_id, row.locale, row.title, row.body, row.tags,
                result.sources.sortedBy { it.position }.map { EducationalSourceData(it.institution, it.title, it.url, it.language) },
                row.updated_at, row.source_checked_on, row.reviewed_on, result.favorite)
        }
}
