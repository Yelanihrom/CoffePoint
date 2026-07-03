package ru.coffeepoint.core.data.repository

import android.content.Context
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.mapper.toInventoryEntity
import ru.coffeepoint.core.data.mapper.toSettingsEntity
import ru.coffeepoint.core.data.mapper.toStationDetail
import ru.coffeepoint.core.data.mapper.toStationEntity
import ru.coffeepoint.feature.station.model.StationDetail

class StationRepository(
    context: Context
) {

    private val db = DatabaseProvider.getDatabase(context)

    private val stationDao = db.coffeeStationDao()
    private val inventoryDao = db.stationInventoryDao()
    private val settingsDao = db.stationSettingsDao()

    suspend fun getStation(id: Int): StationDetail? {

        val station = stationDao.getById(id) ?: return null
        val inventory = inventoryDao.getByStationId(id) ?: return null
        val settings = settingsDao.getByStationId(id) ?: return null

        return toStationDetail(
            station,
            inventory,
            settings
        )

    }

    suspend fun getStations(): List<StationDetail> {

        val result = mutableListOf<StationDetail>()

        stationDao.getAll().forEach { station ->

            val inventory =
                inventoryDao.getByStationId(station.id) ?: return@forEach

            val settings =
                settingsDao.getByStationId(station.id) ?: return@forEach

            result += toStationDetail(
                station,
                inventory,
                settings
            )

        }

        return result

    }

    suspend fun saveStation(detail: StationDetail) {

        stationDao.insert(
            detail.toStationEntity()
        )

        inventoryDao.insert(
            detail.toInventoryEntity()
        )

        settingsDao.insert(
            detail.toSettingsEntity()
        )

    }

    suspend fun addStation(
        detail: StationDetail
    ) {

        val newId = (stationDao.getMaxId() ?: 0) + 1

        saveStation(

            detail.copy(

                station = detail.station.copy(
                    id = newId
                )

            )

        )

    }

    suspend fun deleteStation(
        stationId: Int
    ) {

        inventoryDao.deleteByStationId(stationId)

        settingsDao.deleteByStationId(stationId)

        stationDao.deleteById(stationId)

    }

    suspend fun finishService(
        stationId: Int,
        machinePhotoPath: String?,
        overviewPhotoPath: String?,
        lastServiceDate: String
    ) {

        val detail = getStation(stationId) ?: return

        saveStation(
            detail.copy(
                station = detail.station.copy(
                    lastServiceDate = lastServiceDate,
                    salesAfterService = 0,
                    machinePhotoPath = machinePhotoPath,
                    overviewPhotoPath = overviewPhotoPath
                )
            )
        )

    }

    suspend fun updateSalesAfterService(
        stationId: Int,
        salesAfterService: Int
    ) {

        val detail = getStation(stationId) ?: return

        saveStation(
            detail.copy(
                station = detail.station.copy(
                    salesAfterService = salesAfterService
                )
            )
        )

    }

}