package ru.coffeepoint.core.data.remote.repository

import ru.coffeepoint.core.data.remote.network.ApiProvider

class SalesRepository {

    private val salesApi = ApiProvider.salesApi

    private val token = "f2b7ac204f7d41a7a578cd91"

    suspend fun loadSalesByTerminal(
        dateFrom: String
    ): Map<Long, Int> {

        val response = salesApi.getSalesReport(
            token = token,
            dateFrom = dateFrom
        )

        return response.items.associate { item ->

            item.terminalId to item.incomingCount

        }

    }

}