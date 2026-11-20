package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.EducationalArticle
import com.jecsdev.gestiora.domain.usecases.BrowseEducation
import kotlin.coroutines.cancellation.CancellationException

class EducationViewModel(private val education: BrowseEducation) {
    suspend fun prepare(locale: String, version: Int, articles: List<EducationalArticle>) = capture { education.prepare(locale, version, articles) }
    suspend fun search(locale: String, query: String, favoritesOnly: Boolean) = capture { education.search(locale, query, favoritesOnly) }
    suspend fun setFavorite(id: String, favorite: Boolean) = capture { education.setFavorite(id, favorite) }

    private suspend inline fun <T> capture(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}
