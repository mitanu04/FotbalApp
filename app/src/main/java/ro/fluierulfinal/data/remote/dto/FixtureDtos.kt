package ro.fluierulfinal.data.remote.dto

import kotlinx.serialization.Serializable

// Shapes of one item of /fixtures "response". Only the fields we use are declared;
// the Json instance ignores the rest (ignoreUnknownKeys).

@Serializable
data class FixtureItemDto(
    val fixture: FixtureInfoDto,
    val league: FixtureLeagueDto,
    val teams: FixtureTeamsDto,
    val goals: GoalsDto,
)

@Serializable
data class FixtureInfoDto(
    val id: Int,
    val timestamp: Long,              // kick-off, seconds since 1970 (UTC)
    val venue: VenueRefDto? = null,
    val status: FixtureStatusDto,
)

@Serializable
data class VenueRefDto(
    val id: Int? = null,
    val name: String? = null,
    val city: String? = null,
)

@Serializable
data class FixtureStatusDto(
    val short: String,                // e.g. "NS", "1H", "HT", "FT"
    val elapsed: Int? = null,
)

@Serializable
data class FixtureLeagueDto(
    val id: Int,
    val season: Int,
)

@Serializable
data class FixtureTeamsDto(
    val home: TeamRefDto,
    val away: TeamRefDto,
)

@Serializable
data class TeamRefDto(
    val id: Int,
    val name: String,
    val logo: String? = null,
)

@Serializable
data class GoalsDto(
    val home: Int? = null,
    val away: Int? = null,
)
