package ru.coffeepoint.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.coffeepoint.core.data.local.dao.CoffeeStationDao
import ru.coffeepoint.core.data.local.dao.EmployeeDao
import ru.coffeepoint.core.data.local.dao.EmployeeStationAssignmentDao
import ru.coffeepoint.core.data.local.dao.EmployeeTransactionDao
import ru.coffeepoint.core.data.local.dao.StationInventoryDao
import ru.coffeepoint.core.data.local.dao.StationSettingsDao
import ru.coffeepoint.core.data.local.dao.SupplyCatalogDao
import ru.coffeepoint.core.data.local.dao.WarehouseStockDao
import ru.coffeepoint.core.data.local.entity.CoffeeStationEntity
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import ru.coffeepoint.core.data.local.entity.EmployeeStationAssignmentEntity
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import ru.coffeepoint.core.data.local.entity.StationInventoryEntity
import ru.coffeepoint.core.data.local.entity.StationSettingsEntity
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.core.data.local.entity.WarehouseStockEntity

@Database(
    entities = [
        CoffeeStationEntity::class,
        StationInventoryEntity::class,
        StationSettingsEntity::class,
        SupplyCatalogEntity::class,
        WarehouseStockEntity::class,
        EmployeeEntity::class,
        EmployeeStationAssignmentEntity::class,
        EmployeeTransactionEntity::class,
    ],
    version = 28,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun coffeeStationDao(): CoffeeStationDao

    abstract fun stationInventoryDao(): StationInventoryDao

    abstract fun stationSettingsDao(): StationSettingsDao

    abstract fun supplyCatalogDao(): SupplyCatalogDao

    abstract fun warehouseStockDao(): WarehouseStockDao

    abstract fun employeeDao(): EmployeeDao

    abstract fun employeeStationAssignmentDao(): EmployeeStationAssignmentDao

    abstract fun employeeTransactionDao(): EmployeeTransactionDao

}
