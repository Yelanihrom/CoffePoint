package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.coffeepoint.core.data.local.entity.StationSettingsEntity

@Dao
interface StationSettingsDao {

    @Query("SELECT * FROM station_settings")
    suspend fun getAll(): List<StationSettingsEntity>

    @Query("SELECT * FROM station_settings WHERE stationId = :stationId")
    suspend fun getByStationId(stationId: Int): StationSettingsEntity?

    @Query("DELETE FROM station_settings WHERE stationId = :stationId")
    suspend fun deleteByStationId(stationId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StationSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<StationSettingsEntity>)

}