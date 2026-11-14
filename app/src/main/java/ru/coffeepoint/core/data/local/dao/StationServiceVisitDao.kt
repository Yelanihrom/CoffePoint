package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.coffeepoint.core.data.local.entity.StationServiceVisitEntity
import ru.coffeepoint.core.data.local.entity.ServiceVisitHistoryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface StationServiceVisitDao {
    @Query("""
        SELECT v.id, v.stationId, s.name AS stationName, s.address AS stationAddress,
            v.employeeId, e.name AS employeeName, v.completedAt, v.isSynced, v.syncError
        FROM station_service_visits v
        LEFT JOIN coffee_station s ON s.id = v.stationId
        LEFT JOIN employees e ON e.id = v.employeeId
        ORDER BY julianday(v.completedAt) DESC, v.id
    """)
    fun observeHistory(): Flow<List<ServiceVisitHistoryRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: StationServiceVisitEntity)

    @Query("UPDATE station_service_visits SET syncError = 1, syncErrorType = :errorType WHERE id = :id AND isSynced = 0")
    suspend fun markError(id: String, errorType: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(visit: StationServiceVisitEntity): Long

    @Query("SELECT * FROM station_service_visits WHERE id = :id")
    suspend fun getById(id: String): StationServiceVisitEntity?

    @Query("SELECT * FROM station_service_visits WHERE employeeId = :employeeId ORDER BY completedAt DESC, id")
    suspend fun getByEmployee(employeeId: Int): List<StationServiceVisitEntity>

    @Query("SELECT * FROM station_service_visits WHERE stationId = :stationId ORDER BY completedAt DESC, id")
    suspend fun getByStation(stationId: Int): List<StationServiceVisitEntity>

    @Query("SELECT * FROM station_service_visits WHERE isSynced = 0 ORDER BY completedAt, id")
    suspend fun getUnsynced(): List<StationServiceVisitEntity>
}
