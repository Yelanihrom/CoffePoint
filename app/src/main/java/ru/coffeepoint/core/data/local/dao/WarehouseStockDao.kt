package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.coffeepoint.core.data.local.entity.WarehouseStockEntity

@Dao
interface WarehouseStockDao {

    @Query("SELECT * FROM warehouse_stock ORDER BY itemType")
    suspend fun getAll(): List<WarehouseStockEntity>

    @Query("SELECT * FROM warehouse_stock WHERE isSynced = 0")
    suspend fun getUnsynced(): List<WarehouseStockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WarehouseStockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<WarehouseStockEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(entities: List<WarehouseStockEntity>)

    @Query(
        """
        UPDATE warehouse_stock
        SET
            isSynced = :isSynced,
            syncError = :syncError,
            syncErrorType = :syncErrorType
        WHERE itemType = :itemType
        """
    )
    suspend fun updateSyncStatus(
        itemType: String,
        isSynced: Boolean,
        syncError: Boolean,
        syncErrorType: String
    )

    @Query(
        """
        UPDATE warehouse_stock
        SET
            isSynced = 1,
            syncError = 0,
            syncErrorType = ''
        WHERE isSynced = 0
            AND syncErrorType = :conflictType
        """
    )
    suspend fun markConflictsAsResolvedForImport(conflictType: String)

}
