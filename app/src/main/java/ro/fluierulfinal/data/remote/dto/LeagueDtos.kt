package ro.fluierulfinal.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// One item of /leagues "response": { "league": {...}, "country": {...}, "seasons": [...] }

@Serializable
data class LeagueItemDto(
    val league: LeagueInfoDto,
    val country: CountryDto,
    val seasons: List<SeasonDto> = emptyList(),
)

@Serializable
data class LeagueInfoDto(
    val id: Int,
    val name: String,
    val type: String? = null,         // "League" or "Cup"
    val logo: String? = null,
)

@Serializable
data class CountryDto(
    val name: String,
    val code: String? = null,
)

@Serializable
data class SeasonDto(
    val year: Int,
    val current: Boolean = false,
    val coverage: CoverageDto = CoverageDto(),
)

@Serializable
data class CoverageDto(
    val fixtures: FixtureCoverageDto = FixtureCoverageDto(),
    val odds: Boolean = false,
)

// @SerialName maps the API's snake_case keys to Kotlin camelCase properties.
@Serializable
data class FixtureCoverageDto(
    val events: Boolean = false,
    val lineups: Boolean = false,
    @SerialName("statistics_fixtures") val statisticsFixtures: Boolean = false,
    @SerialName("statistics_players") val statisticsPlayers: Boolean = false,
)
