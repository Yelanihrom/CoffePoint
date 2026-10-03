package ru.coffeepoint.core.sync.google.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import ru.coffeepoint.core.sync.google.dto.GoogleSheetsFullSyncResponseDto
import ru.coffeepoint.core.sync.google.dto.GoogleSheetsSyncRequestDto
import ru.coffeepoint.core.sync.google.dto.GoogleSheetsSyncResponseDto

interface GoogleSheetsSyncApi {

    @GET
    suspend fun fullSync(
        @Url url: String,
        @Query("action") action: String,
        @Query("token") token: String,
        @Query("entities") entities: String? = null
    ): GoogleSheetsFullSyncResponseDto

    @POST
    suspend fun upsert(
        @Url url: String,
        @Body request: GoogleSheetsSyncRequestDto
    ): GoogleSheetsSyncResponseDto

}
