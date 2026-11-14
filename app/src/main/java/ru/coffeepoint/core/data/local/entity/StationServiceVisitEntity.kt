package ru.coffeepoint.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "station_service_visits",
    indices = [Index(value = ["employeeId", "completedAt"]), Index(value = ["stationId", "completedAt"])]
)
data class StationServiceVisitEntity(
    @PrimaryKey val id: String,
    val employeeId: Int,
    val stationId: Int,
    val completedAt: String,
    val createdAt: String? = null,
    val updatedAt: String,
    @ColumnInfo(defaultValue = "0") val isSynced: Boolean = false,
    @ColumnInfo(defaultValue = "0") val syncError: Boolean = false,
    @ColumnInfo(defaultValue = "''") val syncErrorType: String = ""
)
