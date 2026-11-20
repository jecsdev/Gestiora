package com.jecsdev.gestiora.data.datasources.core.education

import androidx.room.*
import com.jecsdev.gestiora.data.models.EducationalContentData
import com.jecsdev.gestiora.data.models.educationSearchText

@Entity(tableName = "educational_content")
data class EducationalContentRow(
    @PrimaryKey val key: String,
    val content_id: String, val locale: String, val title: String, val body: String, val tags: String,
    val search_text: String, val updated_at: Long, val source_checked_on: String, val reviewed_on: String?,
)

@Entity(tableName = "educational_content_source", primaryKeys = ["content_key", "position"])
data class EducationalSourceRow(
    val content_key: String, val position: Int, val institution: String,
    val title: String, val url: String, val language: String,
)

// No foreign key or cascade: editorial updates cannot erase a user's saved state.
@Entity(tableName = "educational_content_user_state")
data class EducationalFavoriteRow(@PrimaryKey val content_id: String, val is_favorite: Boolean)

@Entity(tableName = "educational_seed")
data class EducationalSeedRow(@PrimaryKey val locale: String, val version: Int)

data class EducationalResult(
    @Embedded val content: EducationalContentRow,
    @Relation(parentColumn = "key", entityColumn = "content_key") val sources: List<EducationalSourceRow>,
    val favorite: Boolean,
)

@Dao
abstract class EducationalDao {
    @Transaction
    @Query("""
        SELECT c.*, COALESCE(f.is_favorite, 0) AS favorite FROM educational_content c
        LEFT JOIN educational_content_user_state f ON c.content_id = f.content_id
        WHERE c.locale = :locale AND instr(c.search_text, :query) > 0
          AND (:favoritesOnly = 0 OR f.is_favorite = 1)
        ORDER BY c.content_id
    """)
    abstract suspend fun search(locale: String, query: String, favoritesOnly: Boolean): List<EducationalResult>

    @Query("SELECT * FROM educational_content WHERE `key` = :key")
    abstract suspend fun content(key: String): EducationalContentRow?

    @Query("SELECT version FROM educational_seed WHERE locale = :locale")
    abstract suspend fun seedVersion(locale: String): Int?

    @Upsert abstract suspend fun saveContent(row: EducationalContentRow)
    @Upsert abstract suspend fun saveSeed(row: EducationalSeedRow)
    @Upsert abstract suspend fun saveFavorite(row: EducationalFavoriteRow)
    @Insert abstract suspend fun saveSources(rows: List<EducationalSourceRow>)

    @Query("DELETE FROM educational_content_source WHERE content_key = :key")
    abstract suspend fun deleteSources(key: String)

    @Transaction
    open suspend fun seed(locale: String, version: Int, articles: List<EducationalContentData>) {
        if ((seedVersion(locale) ?: 0) >= version) return
        articles.forEach { article ->
            val key = "$locale:${article.id}"
            val existing = content(key)
            // An older bundled catalog must never replace a newer editorial revision.
            if (existing == null || existing.updated_at < article.updatedAt) {
                saveContent(EducationalContentRow(key, article.id, locale, article.title, article.body,
                    article.tags, educationSearchText("${article.title} ${article.body} ${article.tags}"),
                    article.updatedAt, article.sourceCheckedOn, article.reviewedOn))
                deleteSources(key)
                saveSources(article.sources.mapIndexed { index, source ->
                    EducationalSourceRow(key, index, source.institution, source.title, source.url, source.language)
                })
            }
        }
        saveSeed(EducationalSeedRow(locale, version))
    }
}

/** Separate public-content database; never opens or modifies clinical ciphertext tables. */
@Database(entities = [EducationalContentRow::class, EducationalSourceRow::class, EducationalFavoriteRow::class, EducationalSeedRow::class], version = 1, exportSchema = true)
@ConstructedBy(EducationalDatabaseConstructor::class)
abstract class EducationalDatabase : RoomDatabase() {
    abstract fun catalog(): EducationalDao
}

@Suppress("KotlinNoActualForExpect")
expect object EducationalDatabaseConstructor : RoomDatabaseConstructor<EducationalDatabase> {
    override fun initialize(): EducationalDatabase
}
