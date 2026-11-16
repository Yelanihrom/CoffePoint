package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.coffeepoint.core.data.local.entity.StationMaintenanceEntity

@Dao
interface StationMaintenanceDao {
    @Query("SELECT * FROM station_maintenance WHERE stationId = :stationId ORDER BY nodeType")
    suspend fun getByStationId(stationId: Int): List<StationMaintenanceEntity>

    @Query("SELECT * FROM station_maintenance WHERE stationId = :stationId AND nodeType = :nodeType")
    suspend fun get(stationId: Int, nodeType: String): StationMaintenanceEntity?

    @Query("SELECT * FROM station_maintenance WHERE isSynced = 0")
    suspend fun getUnsynced(): List<StationMaintenanceEntity>

    @Upsert
    suspend fun upsert(entity: StationMaintenanceEntity)

    @Upsert
    suspend fun upsertAll(entities: List<StationMaintenanceEntity>)

    @Query("""UPDATE station_maintenance SET isSynced = 1, syncError = 0, syncErrorType = '',
        pendingFullStationSync = 0, pendingResetSales = 0 WHERE stationId = :stationId AND nodeType = :nodeType
        AND updatedAt = :updatedAt AND lastServiceDate = :serviceDate""")
    suspend fun acknowledge(stationId: Int, nodeType: String, updatedAt: String, serviceDate: String)

    @Query("""UPDATE station_maintenance SET syncError = 1, syncErrorType = :errorType
        WHERE stationId = :stationId AND nodeType = :nodeType AND isSynced = 0
        AND updatedAt = :updatedAt AND lastServiceDate = :serviceDate""")
    suspend fun markError(stationId: Int, nodeType: String, updatedAt: String, serviceDate: String, errorType: String)

    @Query("""UPDATE station_maintenance SET isSynced = 1, syncError = 0, syncErrorType = '',
        pendingFullStationSync = 0, pendingResetSales = 0 WHERE isSynced = 0 AND syncErrorType = :conflictType""")
    suspend fun markConflictsAsResolvedForImport(conflictType: String)
}
