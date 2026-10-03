package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.coffeepoint.core.data.local.entity.StationInventoryEntity

@Dao
interface StationInventoryDao {

    @Query("SELECT * FROM station_inventory")
    suspend fun getAll(): List<StationInventoryEntity>

    @Query("SELECT * FROM station_inventory WHERE isSynced = 0")
    suspend fun getUnsynced(): List<StationInventoryEntity>

    @Query("SELECT * FROM station_inventory WHERE stationId = :stationId")
    suspend fun getByStationId(stationId: Int): StationInventoryEntity?

    @Query("DELETE FROM station_inventory WHERE stationId = :stationId")
    suspend fun deleteByStationId(stationId: Int)

    @Query("DELETE FROM station_inventory")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StationInventoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<StationInventoryEntity>)

    @Query("UPDATE station_inventory SET isSynced = :isSynced, syncError = :syncError, syncErrorType = :syncErrorType WHERE stationId = :stationId")
    suspend fun updateSyncStatus(
        stationId: Int,
        isSynced: Boolean,
        syncError: Boolean,
        syncErrorType: String
    )

    @Query("UPDATE station_inventory SET isSynced = 1, syncError = 0, syncErrorType = '' WHERE isSynced = 0 AND syncErrorType = :conflictType")
    suspend fun markConflictsAsResolvedForImport(conflictType: String)

}
