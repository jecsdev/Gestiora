package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.EducationalStore
import com.jecsdev.gestiora.data.models.*
import com.jecsdev.gestiora.domain.models.*
import com.jecsdev.gestiora.domain.repository.EducationalRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalEducationalRepository(private val store: EducationalStore) : EducationalRepository {
    private val mutex = Mutex()

    override suspend fun prepare(locale: String, version: Int, articles: List<EducationalArticle>) = mutex.withLock {
        requireLocale(locale)
        require(version > 0 && articles.size in 1..100)
        require(articles.map { it.id }.distinct().size == articles.size)
        articles.forEach { article ->
            require(article.locale == locale && article.id.matches(Regex("[a-z0-9-]{1,80}")))
            require(article.title.isNotBlank() && article.title.length <= 300)
            require(article.body.isNotBlank() && article.body.length <= 20_000 && article.tags.length <= 1000)
            require(article.updatedAt in 1..253402300799999L && validCivilDate(article.sourceCheckedOn))
            article.reviewedOn?.let { require(validCivilDate(it)) }
            require(article.sources.size in 1..10)
            article.sources.forEach { source ->
                require(source.institution.isNotBlank() && source.institution.length <= 200)
                require(source.title.isNotBlank() && source.title.length <= 1000)
                require(source.url.matches(Regex("https://(?:www\\.)?(?:who\\.int|unicef\\.org|nhs\\.uk)/[^\\s]+")) && source.url.length <= 2048)
                require(source.language in listOf("es", "en", "fr"))
            }
        }
        // Favorite state supplied by packaged or future remote content is deliberately ignored.
        store.seed(locale, version, articles.map { article -> EducationalContentData(
            article.id, article.locale, article.title, article.body, article.tags,
            article.sources.map { EducationalSourceData(it.institution, it.title, it.url, it.language) },
            article.updatedAt, article.sourceCheckedOn, article.reviewedOn,
        ) })
    }

    override suspend fun search(locale: String, query: String, favoritesOnly: Boolean): List<EducationalArticle> = mutex.withLock {
        requireLocale(locale)
        require(query.length <= 256)
        store.search(locale, educationSearchText(query), favoritesOnly).map { article -> EducationalArticle(
            article.id, article.locale, article.title, article.body, article.tags,
            article.sources.map { ArticleSource(it.institution, it.title, it.url, it.language) },
            article.updatedAt, article.sourceCheckedOn, article.reviewedOn, article.isFavorite,
        ) }
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) = mutex.withLock {
        require(id.matches(Regex("[a-z0-9-]{1,80}")))
        store.setFavorite(id, favorite)
    }

    private fun requireLocale(locale: String) { require(locale in listOf("es", "en", "fr")) }
}
