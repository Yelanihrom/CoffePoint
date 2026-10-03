package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity

@Dao
interface SupplyCatalogDao {

    @Query("SELECT * FROM supply_catalog ORDER BY itemType, priority, id")
    suspend fun getAll(): List<SupplyCatalogEntity>

    @Query("SELECT * FROM supply_catalog WHERE isSynced = 0")
    suspend fun getUnsynced(): List<SupplyCatalogEntity>

    @Query("SELECT * FROM supply_catalog WHERE itemType = :itemType AND isActive = 1 ORDER BY priority, id")
    suspend fun getActiveByItemType(itemType: String): List<SupplyCatalogEntity>

    @Query("SELECT * FROM supply_catalog WHERE id = :id")
    suspend fun getById(id: Int): SupplyCatalogEntity?

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM supply_catalog")
    suspend fun getNextId(): Int

    @Query("DELETE FROM supply_catalog")
    suspend fun deleteAll()

    @Query("DELETE FROM supply_catalog WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SupplyCatalogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SupplyCatalogEntity>)

    @Query("UPDATE supply_catalog SET isSynced = :isSynced, syncError = :syncError, syncErrorType = :syncErrorType WHERE id = :id")
    suspend fun updateSyncStatus(
        id: Int,
        isSynced: Boolean,
        syncError: Boolean,
        syncErrorType: String
    )

    @Query("UPDATE supply_catalog SET isSynced = 1, syncError = 0, syncErrorType = '' WHERE isSynced = 0 AND syncErrorType = :conflictType")
    suspend fun markConflictsAsResolvedForImport(conflictType: String)

}
