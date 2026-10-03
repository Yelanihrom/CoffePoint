package ru.coffeepoint.core.data.repository

import android.content.Context
import ru.coffeepoint.core.data.preferences.SalesRefreshPreferences
import ru.coffeepoint.core.data.remote.repository.SalesRepository
import ru.coffeepoint.core.network.NetworkChecker
import ru.coffeepoint.core.util.toApiDateFormat
import java.io.IOException

class SalesRefreshRepository(
    context: Context
) {

    private val stationRepository = StationRepository(context)
    private val salesRepository = SalesRepository()
    private val networkChecker = NetworkChecker(context)
    private val preferences = SalesRefreshPreferences(context)

    suspend fun refreshSales(): SalesRefreshResult {

        if (!networkChecker.hasInternetConnection()) {
            return SalesRefreshResult.NoInternet
        }

        return try {

            val currentStations = stationRepository.getStations()

            val missingTerminalNames = currentStations
                .filter { detail ->
                    detail.station.lastServiceDate.isNotBlank() &&
                        detail.station.terminalId == null
                }
                .map { detail ->
                    detail.station.name
                }

            currentStations.forEach { detail ->

                if (detail.station.lastServiceDate.isBlank()) return@forEach

                val terminalId = detail.station.terminalId
                    ?: return@forEach

                val apiDate = toApiDateFormat(detail.station.lastServiceDate)

                val salesByTerminal = salesRepository.loadSalesByTerminal(
                    dateFrom = apiDate
                )

                val salesCount = salesByTerminal[terminalId] ?: 0

                stationRepository.updateSalesAfterService(
                    stationId = detail.station.id,
                    salesAfterService = salesCount
                )

            }

            preferences.saveLastRefreshTime(
                System.currentTimeMillis()
            )

            SalesRefreshResult.Success(
                missingTerminalNames = missingTerminalNames
            )

        } catch (_: IOException) {

            SalesRefreshResult.ConnectionError

        }

    }

    fun getLastRefreshText(): String? {

        return preferences.getLastRefreshText()

    }

}

sealed interface SalesRefreshResult {

    data class Success(
        val missingTerminalNames: List<String>
    ) : SalesRefreshResult

    data object NoInternet : SalesRefreshResult

    data object ConnectionError : SalesRefreshResult

}
