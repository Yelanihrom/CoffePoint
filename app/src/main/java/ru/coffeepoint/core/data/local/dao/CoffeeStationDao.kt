package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.coffeepoint.core.data.local.entity.CoffeeStationEntity

@Dao
interface CoffeeStationDao {

    @Query("SELECT * FROM coffee_station")
    suspend fun getAll(): List<CoffeeStationEntity>

    @Query("SELECT * FROM coffee_station WHERE isSynced = 0")
    suspend fun getUnsynced(): List<CoffeeStationEntity>

    @Query("SELECT * FROM coffee_station WHERE id = :id")
    suspend fun getById(id: Int): CoffeeStationEntity?

    @Query("DELETE FROM coffee_station WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM coffee_station")
    suspend fun deleteAll()

    @Query("SELECT MAX(id) FROM coffee_station")
    suspend fun getMaxId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CoffeeStationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CoffeeStationEntity>)

    @Query("UPDATE coffee_station SET isSynced = :isSynced, syncError = :syncError, syncErrorType = :syncErrorType WHERE id = :id")
    suspend fun updateSyncStatus(
        id: Int,
        isSynced: Boolean,
        syncError: Boolean,
        syncErrorType: String
    )

    @Query("UPDATE coffee_station SET pendingServiceOnly = 0, isSynced = CASE WHEN pendingFullStationSync = 1 OR pendingSupplyServiceOnly = 1 THEN 0 ELSE 1 END, syncError = 0, syncErrorType = '' WHERE id = :id")
    suspend fun markServiceFieldsSynced(id: Int)

    @Query("UPDATE coffee_station SET pendingSupplyServiceOnly = 0, pendingMixersServiceDate = 0, pendingLiquidWasteContainerServiceDate = 0, isSynced = CASE WHEN pendingFullStationSync = 1 OR pendingServiceOnly = 1 THEN 0 ELSE 1 END, syncError = 0, syncErrorType = '' WHERE id = :id")
    suspend fun markSupplyServiceFieldsSynced(id: Int)

    @Query("UPDATE coffee_station SET pendingServiceOnly = 0 WHERE id = :id")
    suspend fun clearPendingServiceOnly(id: Int)

    @Query("UPDATE coffee_station SET pendingFullStationSync = 0 WHERE id = :id")
    suspend fun clearPendingFullStationSync(id: Int)

    @Query("UPDATE coffee_station SET isSynced = 1, syncError = 0, syncErrorType = '', pendingServiceOnly = 0, pendingFullStationSync = 0, pendingSupplyServiceOnly = 0, pendingMixersServiceDate = 0, pendingLiquidWasteContainerServiceDate = 0 WHERE isSynced = 0 AND syncErrorType = :conflictType")
    suspend fun markConflictsAsResolvedForImport(conflictType: String)

}
