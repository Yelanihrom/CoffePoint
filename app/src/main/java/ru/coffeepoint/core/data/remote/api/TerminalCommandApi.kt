package ru.coffeepoint.core.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import ru.coffeepoint.core.data.remote.dto.TerminalCommandRequestDto

interface TerminalCommandApi {

    @POST("terminals/{id}/commands")
    suspend fun sendCommand(
        @Path("id")
        terminalId: Long,
        @Query("token")
        token: String,
        @Body
        command: TerminalCommandRequestDto
    )

}
