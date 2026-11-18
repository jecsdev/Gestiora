package com.jecsdev.gestiora.data.datasources.core.education

import com.jecsdev.gestiora.data.models.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/** Tests seed revision policy with a fake DAO; real SQLite rollback has a device fixture. */
class EducationalSeedTest {
    private class Dao : EducationalDao() {
        val contents = mutableMapOf<String, EducationalContentRow>()
        val sources = mutableListOf<EducationalSourceRow>()
        val versions = mutableMapOf<String, Int>()
        val favorites = mutableMapOf<String, Boolean>()
        override suspend fun search(locale: String, query: String, favoritesOnly: Boolean) = error("Use SQLite fixture for SQL behavior")
        override suspend fun content(key: String) = contents[key]
        override suspend fun seedVersion(locale: String) = versions[locale]
        override suspend fun saveContent(row: EducationalContentRow) { contents[row.key] = row }
        override suspend fun saveSeed(row: EducationalSeedRow) { versions[row.locale] = row.version }
        override suspend fun saveFavorite(row: EducationalFavoriteRow) { favorites[row.content_id] = row.is_favorite }
        override suspend fun saveSources(rows: List<EducationalSourceRow>) { sources.addAll(rows) }
        override suspend fun deleteSources(key: String) { sources.removeAll { it.content_key == key } }
    }
    private fun article(locale: String = "es", title: String = "Bebé", time: Long = 1) = EducationalContentData(
        "development", locale, title, "Atención prenatal", "Échographie", listOf(
            EducationalSourceData("OMS", "First source", "https://www.who.int/a", "en"),
            EducationalSourceData("UNICEF", "Second source", "https://www.unicef.org/b", "fr"),
        ), time, "2026-11-16", null,
    )

    @Test fun seedUpdatesSourcesAndSearchIndexWithoutTouchingFavoritesOrOtherLocales() = runBlocking {
        val dao = Dao()
        dao.saveFavorite(EducationalFavoriteRow("development", true))
        dao.seed("es", 1, listOf(article()))
        dao.seed("fr", 1, listOf(article("fr")))
        assertEquals("bebe atencion prenatal echographie", dao.contents.getValue("es:development").search_text)
        dao.seed("es", 2, listOf(article(title = "Updated", time = 2).copy(sources = article().sources.take(1))))
        assertEquals("Updated", dao.contents.getValue("es:development").title)
        assertEquals("Bebé", dao.contents.getValue("fr:development").title)
        assertEquals(1, dao.sources.count { it.content_key == "es:development" })
        assertEquals(2, dao.sources.count { it.content_key == "fr:development" })
        assertEquals(mapOf("development" to true), dao.favorites)
        assertEquals(mapOf("es" to 2, "fr" to 1), dao.versions)
    }

    @Test fun repeatedOrOlderBundledRevisionsNeverReplaceNewerContentOrDuplicateSources() = runBlocking {
        val dao = Dao()
        dao.seed("es", 1, listOf(article(time = 100)))
        dao.seed("es", 1, listOf(article(title = "same version", time = 200)))
        assertEquals(100L, dao.contents.getValue("es:development").updated_at)
        dao.seed("es", 2, listOf(article(title = "older bundled revision", time = 99)))
        assertEquals("Bebé", dao.contents.getValue("es:development").title)
        assertEquals(2, dao.sources.size)
        assertEquals(2, dao.seedVersion("es"))
    }
}
