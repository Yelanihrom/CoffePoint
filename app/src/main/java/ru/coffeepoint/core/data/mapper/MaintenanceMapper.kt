package ru.coffeepoint.core.data.mapper

import ru.coffeepoint.core.data.local.dao.StationMaintenanceDao
import ru.coffeepoint.core.data.local.entity.StationMaintenanceEntity
import ru.coffeepoint.feature.station.model.CoffeeStation
import ru.coffeepoint.feature.station.model.TechServiceNode

fun CoffeeStation.maintenanceDate(node: TechServiceNode): String = when (node) {
    TechServiceNode.MIXERS -> mixersServiceDate
    TechServiceNode.LIQUID_WASTE_CONTAINER -> liquidWasteContainerServiceDate
    TechServiceNode.BREW_UNIT -> brewUnitServiceDate
    TechServiceNode.COFFEE_GRINDER -> coffeeGrinderServiceDate
    TechServiceNode.EXPANSION_TANK -> expansionTankServiceDate
    TechServiceNode.EXHAUST -> exhaustServiceDate
}

fun CoffeeStation.toMaintenanceEntities(): List<StationMaintenanceEntity> = TechServiceNode.entries.map { node ->
    StationMaintenanceEntity(id, node.name, maintenanceDate(node))
}

suspend fun StationMaintenanceDao.importMaintenance(stationId: Int, rows: List<StationMaintenanceEntity>) {
    rows.filter { it.stationId == stationId }.forEach { remote ->
        val local = get(stationId, remote.nodeType)
        upsert(local?.withRemoteMaintenance(remote) ?: remote)
    }
}

fun maintenanceInstant(value: String): java.time.Instant? = runCatching { java.time.Instant.parse(value) }.getOrElse {
    runCatching { java.time.LocalDateTime.parse(value).atZone(java.time.ZoneId.of("Europe/Moscow")).toInstant() }.getOrNull()
}?.truncatedTo(java.time.temporal.ChronoUnit.MILLIS)

fun maintenanceDisplayDate(value: String): String = maintenanceInstant(value)?.atZone(java.time.ZoneId.of("Europe/Moscow"))
    ?.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: value

internal fun StationMaintenanceEntity.withRemoteMaintenance(remote: StationMaintenanceEntity): StationMaintenanceEntity {
    if (!isSynced || stationId != remote.stationId || nodeType != remote.nodeType) return this
    val version = maintenanceInstant(remote.updatedAt) ?: return this
    val localVersion = maintenanceInstant(updatedAt)
    if (localVersion != null && version < localVersion) return this
    if (localVersion == version) {
        if (maintenanceDisplayDate(lastServiceDate) != maintenanceDisplayDate(remote.lastServiceDate)) return this
        if (salesAfterService != null && remote.salesAfterService == null) return this
        val calculated = maintenanceInstant(remote.salesUpdatedAt)
        val localCalculated = maintenanceInstant(salesUpdatedAt)
        if (remote.salesAfterService != null && calculated == null) return this
        if (localCalculated != null && (calculated == null || calculated < localCalculated)) return this
    }
    return remote
}

fun CoffeeStation.withMaintenance(rows: List<StationMaintenanceEntity>): CoffeeStation {
    val nodes = rows.associateBy { it.nodeType }
    fun date(node: TechServiceNode) = maintenanceDisplayDate(nodes[node.name]?.lastServiceDate.orEmpty())
    val pending = rows.filter { !it.isSynced }
    val error = pending.firstOrNull { it.syncError }
    return copy(
        maintenanceSales = TechServiceNode.entries.mapNotNull { node ->
            nodes[node.name]?.salesAfterService?.let { node to it }
        }.toMap(),
        mixersServiceDate = date(TechServiceNode.MIXERS),
        liquidWasteContainerServiceDate = date(TechServiceNode.LIQUID_WASTE_CONTAINER),
        brewUnitServiceDate = date(TechServiceNode.BREW_UNIT),
        coffeeGrinderServiceDate = date(TechServiceNode.COFFEE_GRINDER),
        expansionTankServiceDate = date(TechServiceNode.EXPANSION_TANK),
        exhaustServiceDate = date(TechServiceNode.EXHAUST),
        isSynced = isSynced && pending.isEmpty(),
        syncError = syncError || error != null,
        syncErrorType = if (syncError) syncErrorType else error?.syncErrorType.orEmpty(),
        pendingFullStationSync = pendingFullStationSync || pending.any { it.pendingFullStationSync },
        pendingSupplyServiceOnly = pendingSupplyServiceOnly || pending.any { !it.pendingFullStationSync },
        pendingMixersServiceDate = pendingMixersServiceDate || pending.any {
            it.nodeType == TechServiceNode.MIXERS.name && !it.pendingFullStationSync },
        pendingLiquidWasteContainerServiceDate = pendingLiquidWasteContainerServiceDate || pending.any {
            it.nodeType == TechServiceNode.LIQUID_WASTE_CONTAINER.name && !it.pendingFullStationSync }
    )
}
