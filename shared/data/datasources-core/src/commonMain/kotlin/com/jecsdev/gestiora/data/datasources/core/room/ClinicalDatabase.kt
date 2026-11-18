package com.jecsdev.gestiora.data.datasources.core.room

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Transaction
import androidx.room.Upsert

/** Only authenticated ciphertext is stored here. Clinical fields are not SQL-indexed yet. */
@Entity(tableName = "protected_payloads")
data class ProtectedPayload(@PrimaryKey val store: String, val ciphertext: ByteArray)

@Entity(tableName = "legacy_imports")
data class LegacyImport(@PrimaryKey val id: String)

@Dao
abstract class ProtectedPayloadDao {
    @Query("SELECT * FROM protected_payloads WHERE store = :store")
    abstract suspend fun read(store: String): ProtectedPayload?

    @Query("SELECT COUNT(*) FROM protected_payloads")
    abstract suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM legacy_imports WHERE id = :id)")
    abstract suspend fun imported(id: String): Boolean

    @Insert
    abstract suspend fun insert(payloads: List<ProtectedPayload>)

    @Insert
    abstract suspend fun markImported(marker: LegacyImport)

    @Upsert
    abstract suspend fun write(payload: ProtectedPayload)

    @Query("DELETE FROM protected_payloads WHERE store = :store")
    abstract suspend fun delete(store: String)

    @Transaction
    open suspend fun importOnce(id: String, payloads: List<ProtectedPayload>) {
        if (imported(id)) return
        check(count() == 0) { "Unmarked clinical database contains data; import refused" }
        insert(payloads)
        markImported(LegacyImport(id))
    }

    @Transaction
    open suspend fun importCollections(id: String, stores: List<String>, payloads: List<ProtectedPayload>) {
        if (imported(id)) return
        require(stores.isNotEmpty() && stores.distinct().size == stores.size)
        require(payloads.all { it.store in stores })
        check(imported(ProtectedClinicalStorage.IMPORT_ID)) { "Initial clinical import is required" }
        check(stores.all { read(it) == null }) { "Unmarked clinical collection contains data; import refused" }
        insert(payloads)
        markImported(LegacyImport(id))
    }
}

@Database(entities = [ProtectedPayload::class, LegacyImport::class], version = 1, exportSchema = true)
@ConstructedBy(ClinicalDatabaseConstructor::class)
abstract class ClinicalDatabase : RoomDatabase() {
    abstract fun payloads(): ProtectedPayloadDao
}

@Suppress("KotlinNoActualForExpect")
expect object ClinicalDatabaseConstructor : RoomDatabaseConstructor<ClinicalDatabase> {
    override fun initialize(): ClinicalDatabase
}
