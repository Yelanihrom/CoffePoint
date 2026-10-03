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

    @Query("UPDATE coffee_station SET isSynced = 1, syncError = 0, syncErrorType = '' WHERE isSynced = 0 AND syncErrorType = :conflictType")
    suspend fun markConflictsAsResolvedForImport(conflictType: String)

}
