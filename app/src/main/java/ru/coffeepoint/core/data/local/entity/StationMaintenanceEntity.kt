package ru.coffeepoint.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "station_maintenance",
    primaryKeys = ["stationId", "nodeType"],
    foreignKeys = [ForeignKey(entity = CoffeeStationEntity::class, parentColumns = ["id"],
        childColumns = ["stationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("stationId")]
)
data class StationMaintenanceEntity(
    val stationId: Int,
    val nodeType: String,
    val lastServiceDate: String = "",
    val salesAfterService: Long? = null,
    val salesUpdatedAt: String = "",
    val updatedAt: String = "",
    val isSynced: Boolean = true,
    val syncError: Boolean = false,
    val syncErrorType: String = "",
    val pendingFullStationSync: Boolean = false,
    @androidx.room.ColumnInfo(defaultValue = "0") val pendingResetSales: Boolean = false
)
