package ro.fluierulfinal.data.remote.dto

import kotlinx.serialization.Serializable

// One item of /teams "response": { "team": {...}, "venue": {...} }

@Serializable
data class TeamItemDto(
    val team: TeamDto,
    val venue: VenueRefDto? = null,
)

@Serializable
data class TeamDto(
    val id: Int,
    val name: String,
    val code: String? = null,
    val country: String? = null,
    val logo: String? = null,
)
