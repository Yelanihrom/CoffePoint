package ru.coffeepoint.core.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SalesResponseDto(

    @SerializedName("total")
    val total: SalesTotalDto,

    @SerializedName("page_number")
    val pageNumber: Int,

    @SerializedName("items_per_page")
    val itemsPerPage: Int,

    @SerializedName("items_count")
    val itemsCount: Int,

    @SerializedName("items")
    val items: List<SalesItemDto>,

    @SerializedName("success")
    val success: Boolean

)

data class SalesTotalDto(

    @SerializedName("incoming_amount")
    val incomingAmount: Int,

    @SerializedName("incoming_count")
    val incomingCount: Int,

    @SerializedName("cancelled_amount")
    val cancelledAmount: Int,

    @SerializedName("cancelled_count")
    val cancelledCount: Int,

    @SerializedName("rejected_amount")
    val rejectedAmount: Int,

    @SerializedName("rejected_count")
    val rejectedCount: Int

)

data class SalesItemDto(

    @SerializedName("terminal_id")
    val terminalId: Long,

    @SerializedName("tid")
    val tid: String,

    @SerializedName("terminal_comment")
    val terminalComment: String,

    @SerializedName("incoming_amount")
    val incomingAmount: Int,

    @SerializedName("incoming_count")
    val incomingCount: Int,

    @SerializedName("cancelled_amount")
    val cancelledAmount: Int,

    @SerializedName("cancelled_count")
    val cancelledCount: Int,

    @SerializedName("rejected_amount")
    val rejectedAmount: Int,

    @SerializedName("rejected_count")
    val rejectedCount: Int

)