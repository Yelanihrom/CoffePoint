package ru.coffeepoint.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "station_settings")
data class StationSettingsEntity(

    @PrimaryKey
    val stationId: Int,

    val cupsMax: Int,

    val lidsMax: Int,

    val coffeeMax: Int,

    val milkMax: Int,

    val chocolateMax: Int,

    val toppingMax: Int,

    val sugarMax: Int = 2,

    val stirrersMax: Int = 4,

    val garbageBagsMax: Int = 2,

    val updatedAt: String = "",

    val isSynced: Boolean = true,

    val syncError: Boolean = false,

    val syncErrorType: String = ""

)
