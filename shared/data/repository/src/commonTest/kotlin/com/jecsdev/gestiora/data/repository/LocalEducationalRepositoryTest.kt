package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.EducationalStore
import com.jecsdev.gestiora.data.models.*
import com.jecsdev.gestiora.domain.models.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class LocalEducationalRepositoryTest {
    private class Store : EducationalStore {
        var seeded: List<EducationalContentData> = emptyList()
        var lastQuery = ""
        var lastLocale = ""
        var favoritesOnly = false
        var fail = false
        var writes = 0
        override suspend fun seed(locale: String, version: Int, articles: List<EducationalContentData>) {
            check(!fail); seeded = articles; writes++
        }
        override suspend fun search(locale: String, normalizedQuery: String, favoritesOnly: Boolean): List<EducationalContentData> {
            check(!fail); lastQuery = normalizedQuery; lastLocale = locale; this.favoritesOnly = favoritesOnly
            return seeded.map { it.copy(isFavorite = true) }
        }
        override suspend fun setFavorite(id: String, favorite: Boolean) { check(!fail); writes++ }
    }
    private fun article(locale: String = "es") = EducationalArticle("movements", locale, "Bebé", "Ecografía y movimientos",
        "atención prenatal", listOf(ArticleSource("OMS", "Recomendaciones", "https://www.who.int/example", "en")),
        1794787200000L, "2026-11-16")

    @Test fun searchFoldsSpanishFrenchCombiningMarksAndKeepsWildcardCharactersLiteral() = runBlocking {
        val store = Store()
        val repository = LocalEducationalRepository(store)
        val examples = mapOf("  ÉCHOGRAPHIE  BÉBÉ " to "echographie bebe", "Atencio\u0301n Ñ" to "atencion n",
            "CŒUR, déjà / %_\\" to "coeur, deja / %_\\", "L’ENFANT" to "l'enfant")
        examples.forEach { (query, expected) ->
            repository.search("fr", query, true)
            assertEquals(expected, store.lastQuery)
            assertEquals("fr", store.lastLocale)
            assertTrue(store.favoritesOnly)
        }
    }

    @Test fun seedIgnoresIncomingFavoritesAndMapsSourcesDatesAndReadingState() = runBlocking {
        val store = Store()
        val repository = LocalEducationalRepository(store)
        val original = article().copy(isFavorite = true)
        repository.prepare("es", 1, listOf(original))
        assertFalse(store.seeded.single().isFavorite)
        assertEquals(original, repository.search("es", "", false).single())
    }

    @Test fun invalidSeedIsRejectedBeforeAnyWriteAndSearchIsBounded() = runBlocking {
        val store = Store()
        val repository = LocalEducationalRepository(store)
        val original = article()
        val invalid = listOf(original.copy(locale = "fr"), original.copy(sourceCheckedOn = "2026-02-30"),
            original.copy(id = "bad:id"), original.copy(body = ""), original.copy(reviewedOn = "bad"),
            original.copy(sources = emptyList()), original.copy(sources = listOf(original.sources.single().copy(url = "https://who.int.evil.example/test"))))
        invalid.forEach { assertFailsWith<IllegalArgumentException> { repository.prepare("es", 1, listOf(it)) } }
        assertFailsWith<IllegalArgumentException> { repository.prepare("es", 1, listOf(original, original)) }
        assertFailsWith<IllegalArgumentException> { repository.search("es", "x".repeat(257), false) }
        assertFailsWith<IllegalArgumentException> { repository.search("de", "", false) }
        assertEquals(0, store.writes)
    }

    @Test fun storageFailurePropagatesWithoutReportingAnEmptyCatalogOrSuccessfulFavorite() = runBlocking {
        val store = Store().apply { fail = true }
        val repository = LocalEducationalRepository(store)
        assertFailsWith<IllegalStateException> { repository.search("es", "", false) }
        assertFailsWith<IllegalStateException> { repository.prepare("es", 1, listOf(article())) }
        assertFailsWith<IllegalStateException> { repository.setFavorite("movements", true) }
        assertEquals(0, store.writes)
    }
}
