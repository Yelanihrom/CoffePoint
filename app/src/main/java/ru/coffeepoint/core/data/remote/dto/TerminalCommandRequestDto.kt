package ru.coffeepoint.core.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TerminalCommandRequestDto(
    @SerializedName("command_id")
    val commandId: Int,
    @SerializedName("parameter1")
    val parameter1: Int? = null
)
