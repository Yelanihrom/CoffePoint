package ru.coffeepoint.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.coffeepoint.core.data.local.converter.StockLevelConverter
import ru.coffeepoint.core.data.local.dao.CoffeeStationDao
import ru.coffeepoint.core.data.local.dao.StationInventoryDao
import ru.coffeepoint.core.data.local.dao.StationSettingsDao
import ru.coffeepoint.core.data.local.entity.CoffeeStationEntity
import ru.coffeepoint.core.data.local.entity.StationInventoryEntity
import ru.coffeepoint.core.data.local.entity.StationSettingsEntity

@Database(
    entities = [
        CoffeeStationEntity::class,
        StationInventoryEntity::class,
        StationSettingsEntity::class,
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(StockLevelConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun coffeeStationDao(): CoffeeStationDao

    abstract fun stationInventoryDao(): StationInventoryDao

    abstract fun stationSettingsDao(): StationSettingsDao

}