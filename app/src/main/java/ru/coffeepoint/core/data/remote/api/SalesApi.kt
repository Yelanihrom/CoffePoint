package ru.coffeepoint.core.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query
import ru.coffeepoint.core.data.remote.dto.SalesResponseDto

interface SalesApi {

    @GET("reports/common")
    suspend fun getSalesReport(

        @Query("token")
        token: String,

        @Query("DateFrom")
        dateFrom: String

    ): SalesResponseDto

}