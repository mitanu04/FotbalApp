package ro.fluierulfinal.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// /status "response": account plan and how many requests were used today. Calling /status is free.

@Serializable
data class StatusDto(
    val subscription: SubscriptionDto,
    val requests: RequestsDto,
)

@Serializable
data class SubscriptionDto(
    val plan: String,
    val active: Boolean,
)

@Serializable
data class RequestsDto(
    val current: Int,
    @SerialName("limit_day") val limitDay: Int,
)
