package ru.coffeepoint.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "station_inventory")
data class StationInventoryEntity(

    @PrimaryKey
    val stationId: Int,

    val cups: Int,

    val lids: Int,

    val coffee: Int,

    val milk: Int,

    val chocolate: Int,

    val topping: Int,

    val sugar: Int,

    val stirrers: Int,

    val garbageBags: Int = 2,

    val updatedAt: String = "",

    val isSynced: Boolean = true,

    val syncError: Boolean = false,

    val syncErrorType: String = ""

)
