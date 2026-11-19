package ro.fluierulfinal.data.remote.dto

import kotlinx.serialization.Serializable

// /standings "response": [ { "league": { "id", "season", "standings": [ [row, row...], [group 2...] ] } } ]
// A league can have several groups (e.g. Liga I: regular season, championship round, relegation round).

@Serializable
data class StandingsItemDto(val league: StandingsLeagueDto)

@Serializable
data class StandingsLeagueDto(
    val id: Int,
    val season: Int,
    val standings: List<List<StandingRowDto>> = emptyList(),
)

@Serializable
data class StandingRowDto(
    val rank: Int,
    val team: TeamRefDto,
    val points: Int,
    val goalsDiff: Int,
    val group: String? = null,
    val form: String? = null,             // last results, most recent last, e.g. "DWWWW"
    val description: String? = null,      // e.g. "UEFA Champions League Qualifiers", "Relegation"
    val all: StandingRecordDto,
)

@Serializable
data class StandingRecordDto(
    val played: Int = 0,
    val win: Int = 0,
    val draw: Int = 0,
    val lose: Int = 0,
    val goals: StandingGoalsDto = StandingGoalsDto(),
)

@Serializable
data class StandingGoalsDto(
    val `for`: Int? = null,
    val against: Int? = null,
)
