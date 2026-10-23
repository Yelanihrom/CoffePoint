package ru.coffeepoint.core.data.repository

import android.content.Context
import androidx.room.withTransaction
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.mapper.toInventoryEntity
import ru.coffeepoint.feature.service.model.warehouseShortages
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.StationInventory
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

sealed interface SupplierRefillSaveResult {
    data object Saved : SupplierRefillSaveResult
    data class Shortage(val quantities: Map<String, Int>) : SupplierRefillSaveResult
}

class SupplierRefillRepository(private val db: AppDatabase) {
    constructor(context: Context) : this(DatabaseProvider.getDatabase(context))

    suspend fun saveLocally(detail: StationDetail, actual: StationInventory, writeOff: Map<String, Int>): SupplierRefillSaveResult {
        require(writeOff.values.all { it > 0 })
        val result = db.withTransaction {
            val inventoryDao = db.stationInventoryDao()
            checkNotNull(inventoryDao.getByStationId(detail.station.id)) { "Station inventory missing" }
            val stockDao = db.warehouseStockDao()
            val stock = if (writeOff.isEmpty()) emptyList() else stockDao.getAll()
            val quantities = stock.associate { it.itemType to it.quantity }
            if (warehouseShortages(writeOff, quantities).isNotEmpty()) {
                return@withTransaction SupplierRefillSaveResult.Shortage(quantities)
            }
            val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC).format(Instant.now())
            inventoryDao.insert(detail.copy(inventory = actual.copy(
                updatedAt = timestamp, isSynced = false, syncError = false, syncErrorType = ""
            )).toInventoryEntity())
            val changedStock = stock.filter { writeOff.containsKey(it.itemType) }.map { item ->
                item.copy(quantity = item.quantity - writeOff.getValue(item.itemType),
                    updatedAt = timestamp, isSynced = false, syncError = false, syncErrorType = "")
            }
            if (changedStock.isNotEmpty()) stockDao.insertAll(changedStock)
            SupplierRefillSaveResult.Saved
        }
        return result
    }
}
