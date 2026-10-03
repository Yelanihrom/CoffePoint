package ru.coffeepoint.core.data.remote.repository

import ru.coffeepoint.core.data.remote.VendistaCredentials
import ru.coffeepoint.core.data.remote.network.ApiProvider

class SalesRepository {

    private val salesApi = ApiProvider.salesApi

    suspend fun loadSalesByTerminal(
        dateFrom: String,
        dateTo: String? = null
    ): Map<Long, Int> {

        val response = salesApi.getSalesReport(
            token = VendistaCredentials.TOKEN,
            dateFrom = dateFrom,
            dateTo = dateTo
        )

        return response.items.associate { item ->

            item.terminalId to item.incomingCount

        }

    }

}
