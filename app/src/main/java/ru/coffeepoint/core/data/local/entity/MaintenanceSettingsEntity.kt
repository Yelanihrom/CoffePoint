package ru.coffeepoint.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "maintenance_settings")
data class MaintenanceSettingsEntity(
    @PrimaryKey val nodeType: String,
    val intervalDays: Int,
    val salesLimit: Long,
    @ColumnInfo(defaultValue = "''") val updatedAt: String = "",
    @ColumnInfo(defaultValue = "1") val isSynced: Boolean = true,
    @ColumnInfo(defaultValue = "0") val syncError: Boolean = false,
    @ColumnInfo(defaultValue = "''") val syncErrorType: String = ""
)
