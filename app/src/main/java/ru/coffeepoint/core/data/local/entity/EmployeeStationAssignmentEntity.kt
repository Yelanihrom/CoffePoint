package ru.coffeepoint.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "employee_station_assignments",
    indices = [
        Index(
            value = [
                "employeeId",
                "stationId",
                "validFrom",
                "validTo",
            ]
        )
    ]
)
data class EmployeeStationAssignmentEntity(
    @PrimaryKey
    val id: String,
    val employeeId: Int,
    val stationId: Int,
    val validFrom: String,
    val validTo: String?,
    val createdAt: String,
    val updatedAt: String,
    val isSynced: Boolean = true,
    val syncError: Boolean = false,
    val syncErrorType: String = ""
)
