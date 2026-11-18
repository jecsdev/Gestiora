package com.jecsdev.gestiora.data.datasources.core.education

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.jecsdev.gestiora.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.util.UUID
import kotlin.test.*

/** Test-package database only. No clinical files, keys or network access. */
class EducationalDatabaseDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().context
    private fun builder(file: File) = Room.databaseBuilder<EducationalDatabase>(context, file.absolutePath)
        .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)
    private inline fun <T> EducationalDatabase.useDatabase(block: (EducationalDatabase) -> T): T =
        try { block(this) } finally { close() }
    private fun article(locale: String, title: String = "Bebé et échographie", id: String = "development", time: Long = 1) =
        EducationalContentData(id, locale, title, "Réponses prénatales", "atención %_ literal",
            listOf(EducationalSourceData("OMS", "Source", "https://www.who.int/example", "en")), time, "2026-11-16", null)

    @Test fun seedSearchAndFavoritesSurviveLocaleUpdatesAndDatabaseReopen() = runBlocking<Unit> {
        val file = File(context.cacheDir, "education-test-${UUID.randomUUID()}.db")
        try {
            builder(file).build().useDatabase { db ->
                val store = RoomEducationalStore(db.catalog())
                listOf("es", "en", "fr").forEach { locale -> store.seed(locale, 1, listOf(article(locale))) }
                assertEquals(1, store.search("es", educationSearchText("BÉBÉ"), false).size)
                assertEquals(1, store.search("fr", educationSearchText("E\u0301CHOGRAPHIE"), false).size)
                assertEquals(1, store.search("en", educationSearchText("ATENCION"), false).size)
                assertEquals(1, store.search("es", "%_", false).size)
                assertTrue(store.search("es", "%%", false).isEmpty())
                assertTrue(store.search("es", "' OR 1=1 --", false).isEmpty())
                assertTrue(store.search("es", "", true).isEmpty())
                store.setFavorite("development", true)
                listOf("es", "en", "fr").forEach { locale ->
                    assertTrue(store.search(locale, "", true).single().isFavorite)
                }
                store.seed("es", 2, listOf(article("es", title = "Updated", time = 2)))
                assertTrue(store.search("es", "updated", true).single().isFavorite)
                // A repeat/older packaged seed must not replace newer content or sources.
                store.seed("es", 1, listOf(article("es", title = "stale")))
                assertTrue(store.search("es", "stale", false).isEmpty())
            }
            builder(file).build().useDatabase { db ->
                val store = RoomEducationalStore(db.catalog())
                val article = store.search("es", "", true).single()
                assertEquals("Updated", article.title)
                assertEquals("https://www.who.int/example", article.sources.single().url)
                assertEquals("2026-11-16", article.sourceCheckedOn)
                assertNull(article.reviewedOn)
                store.setFavorite("development", false)
                assertTrue(store.search("fr", "", true).isEmpty())
            }
        } finally { context.deleteDatabase(file.absolutePath) }
    }

    @Test fun failureMidSeedRollsBackContentSourcesAndVersionThenRetrySucceeds() = runBlocking<Unit> {
        val file = File(context.cacheDir, "education-rollback-${UUID.randomUUID()}.db")
        try {
            builder(file).addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(connection: SQLiteConnection) {
                    connection.execSQL("""CREATE TRIGGER reject_fixture BEFORE INSERT ON educational_content
                        WHEN NEW.content_id = 'reject' BEGIN SELECT RAISE(ABORT, 'fixture failure'); END""")
                }
            }).build().useDatabase { db ->
                val store = RoomEducationalStore(db.catalog())
                store.setFavorite("development", true)
                assertFails { store.seed("es", 1, listOf(article("es"), article("es", id = "reject"))) }
                assertTrue(store.search("es", "", false).isEmpty())
                assertNull(db.catalog().seedVersion("es"))
                store.seed("es", 1, listOf(article("es")))
                assertEquals(1, db.catalog().seedVersion("es"))
                assertTrue(store.search("es", "", true).single().isFavorite)
            }
        } finally { context.deleteDatabase(file.absolutePath) }
    }
}
