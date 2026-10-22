package ru.coffeepoint.core.sync.google

import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.entity.WarehouseStockEntity
import ru.coffeepoint.core.data.local.entity.CoffeeStationEntity
import ru.coffeepoint.core.data.local.entity.StationInventoryEntity
import ru.coffeepoint.core.data.mapper.toInventoryEntity
import ru.coffeepoint.core.data.mapper.toStationDetail
import ru.coffeepoint.core.data.mapper.toStationEntity
import ru.coffeepoint.feature.station.model.CoffeeStation
import ru.coffeepoint.feature.station.model.StationInventory

data class ServiceStartSnapshot(
    val station: CoffeeStation,
    val inventory: StationInventory,
    val warehouse: List<WarehouseStockEntity>
)

data class ServiceStartLocalState(
    val station: CoffeeStationEntity,
    val inventory: StationInventoryEntity,
    val warehouse: List<WarehouseStockEntity>
) {
    val hasPendingChanges: Boolean get() = !station.isSynced || !inventory.isSynced || warehouse.any { !it.isSynced }
}

interface ServiceStartStore {
    suspend fun read(stationId: Int, includeWarehouse: Boolean): ServiceStartLocalState?
    suspend fun applyIfUnchanged(before: ServiceStartLocalState, fresh: ServiceStartSnapshot, includeWarehouse: Boolean): GoogleSheetsImportResult
}

class GoogleSheetsServiceStartImporter(
    private val store: ServiceStartStore,
    private val load: suspend (Int, Boolean) -> ServiceStartSnapshot
) {
    constructor(db: AppDatabase, remote: GoogleSheetsRemoteDataSource = GoogleSheetsRemoteDataSource()) :
        this(RoomServiceStartStore(db), remote::loadServiceStart)

    suspend fun refresh(stationId: Int, includeWarehouse: Boolean): GoogleSheetsImportResult {
        try {
            val before = store.read(stationId, includeWarehouse) ?: return GoogleSheetsImportResult.Empty
            if (before.hasPendingChanges) {
                return GoogleSheetsImportResult.SkippedPendingChanges
            }
            val fresh = load(stationId, includeWarehouse)
            check(fresh.station.id == stationId) { "Wrong station returned" }
            if (includeWarehouse) {
                val receivedTypes = fresh.warehouse.map { it.itemType }.toSet()
                check(receivedTypes.size == fresh.warehouse.size &&
                    receivedTypes.containsAll(before.warehouse.map { it.itemType })) { "Incomplete warehouse snapshot" }
            }
            return store.applyIfUnchanged(before, fresh, includeWarehouse)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return GoogleSheetsImportResult.Error(error)
        }
    }
}

private class RoomServiceStartStore(private val db: AppDatabase) : ServiceStartStore {
    private suspend fun readState(stationId: Int, includeWarehouse: Boolean): ServiceStartLocalState? {
        val station = db.coffeeStationDao().getById(stationId) ?: return null
        val inventory = db.stationInventoryDao().getByStationId(stationId) ?: return null
        return ServiceStartLocalState(station, inventory,
            if (includeWarehouse) db.warehouseStockDao().getAll() else emptyList())
    }

    override suspend fun read(stationId: Int, includeWarehouse: Boolean) = db.withTransaction {
        readState(stationId, includeWarehouse)
    }

    override suspend fun applyIfUnchanged(before: ServiceStartLocalState, fresh: ServiceStartSnapshot, includeWarehouse: Boolean) = db.withTransaction {
        // A worker or another local action may have changed data during the GET.
        if (readState(before.station.id, includeWarehouse) != before) {
            return@withTransaction GoogleSheetsImportResult.SkippedPendingChanges
        }
        val settings = db.stationSettingsDao().getByStationId(before.station.id)
            ?: return@withTransaction GoogleSheetsImportResult.Empty
        val detail = toStationDetail(before.station, before.inventory, settings).copy(
            station = fresh.station, inventory = fresh.inventory)
        db.coffeeStationDao().insert(detail.toStationEntity())
        db.stationInventoryDao().insert(detail.toInventoryEntity())
        if (includeWarehouse) db.warehouseStockDao().insertAll(fresh.warehouse)
        GoogleSheetsImportResult.Success
    }
}
