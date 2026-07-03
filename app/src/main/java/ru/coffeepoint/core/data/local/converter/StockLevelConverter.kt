package ru.coffeepoint.core.data.local.converter

import androidx.room.TypeConverter
import ru.coffeepoint.feature.station.model.StockLevel

class StockLevelConverter {

    @TypeConverter
    fun fromStockLevel(value: StockLevel): String {
        return value.name
    }

    @TypeConverter
    fun toStockLevel(value: String): StockLevel {
        return StockLevel.valueOf(value)
    }

}