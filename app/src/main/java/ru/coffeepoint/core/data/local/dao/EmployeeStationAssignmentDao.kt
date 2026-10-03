package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.coffeepoint.core.data.local.entity.EmployeeStationAssignmentEntity

@Dao
interface EmployeeStationAssignmentDao {

    @Query("SELECT * FROM employee_station_assignments ORDER BY stationId, validFrom")
    suspend fun getAll(): List<EmployeeStationAssignmentEntity>

    @Query(
        """
        SELECT *
        FROM employee_station_assignments
        WHERE employeeId = :employeeId
        ORDER BY validFrom DESC
        """
    )
    suspend fun getByEmployeeId(employeeId: Int): List<EmployeeStationAssignmentEntity>

    @Query(
        """
        SELECT *
        FROM employee_station_assignments
        WHERE stationId = :stationId
        ORDER BY validFrom DESC
        """
    )
    suspend fun getByStationId(stationId: Int): List<EmployeeStationAssignmentEntity>

    @Query(
        """
        SELECT *
        FROM employee_station_assignments
        WHERE stationId = :stationId
            AND validTo IS NULL
        ORDER BY validFrom DESC
        LIMIT 1
        """
    )
    suspend fun getCurrentByStationId(stationId: Int): EmployeeStationAssignmentEntity?

    @Query(
        """
        UPDATE employee_station_assignments
        SET validTo = :validTo,
            updatedAt = :updatedAt
        WHERE stationId = :stationId
            AND validTo IS NULL
        """
    )
    suspend fun closeCurrentForStation(
        stationId: Int,
        validTo: String,
        updatedAt: String
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EmployeeStationAssignmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EmployeeStationAssignmentEntity>)

    @Query("DELETE FROM employee_station_assignments")
    suspend fun deleteAll()

}
