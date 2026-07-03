package ru.coffeepoint.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.coffeepoint.feature.station.model.StockLevel

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

    val sugar: StockLevel,

    val stirrers: StockLevel

)