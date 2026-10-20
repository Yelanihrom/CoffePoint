package ru.coffeepoint.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "station_settings")
data class StationSettingsEntity(

    @PrimaryKey
    val stationId: Int,

    @ColumnInfo(defaultValue = "3")
    val cupsMin: Int = 3,

    val cupsMax: Int,

    @ColumnInfo(defaultValue = "3")
    val lidsMin: Int = 3,

    val lidsMax: Int,

    @ColumnInfo(defaultValue = "3")
    val coffeeMin: Int = 3,

    val coffeeMax: Int,

    @ColumnInfo(defaultValue = "3")
    val milkMin: Int = 3,

    val milkMax: Int,

    @ColumnInfo(defaultValue = "3")
    val chocolateMin: Int = 3,

    val chocolateMax: Int,

    @ColumnInfo(defaultValue = "3")
    val toppingMin: Int = 3,

    val toppingMax: Int,

    @ColumnInfo(defaultValue = "1")
    val sugarMin: Int = 1,

    val sugarMax: Int = 2,

    @ColumnInfo(defaultValue = "1")
    val stirrersMin: Int = 1,

    val stirrersMax: Int = 4,

    @ColumnInfo(defaultValue = "1")
    val garbageBagsMin: Int = 1,

    val garbageBagsMax: Int = 2,

    val updatedAt: String = "",

    val isSynced: Boolean = true,

    val syncError: Boolean = false,

    val syncErrorType: String = ""

)
