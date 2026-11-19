package ro.fluierulfinal.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ro.fluierulfinal.data.remote.dto.FixtureItemDto
import ro.fluierulfinal.data.remote.dto.StandingsItemDto
import ro.fluierulfinal.data.remote.dto.TopScorerItemDto
import ro.fluierulfinal.domain.model.AlertSettings
import ro.fluierulfinal.domain.model.Favorite
import ro.fluierulfinal.domain.model.League
import ro.fluierulfinal.domain.model.LeagueCoverage
import ro.fluierulfinal.domain.model.Team
import java.time.Instant
import java.time.OffsetDateTime

// Rows of the Supabase tables (see supabase/schema.sql), with the column names in snake_case.
// Timestamps travel as ISO-8601 text, e.g. "2026-11-19T21:45:00.123+00:00".

internal fun parseTimestamp(text: String): Instant = OffsetDateTime.parse(text).toInstant()

@Serializable
data class FavoriteRow(
    @SerialName("team_id") val teamId: Int,
    val name: String,
    val country: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("league_id") val leagueId: Int? = null,
    @SerialName("added_at") val addedAt: String,
    val alerts: AlertsJson = AlertsJson(),
    // user_id is not sent: the database fills it with auth.uid() (column default) and RLS checks it
)

@Serializable
data class AlertsJson(
    val goalSound: Boolean = false,
    val redCardVibrate: Boolean = false,
    val penaltyVibrate: Boolean = false,
    val lineups: Boolean = false,
)

fun FavoriteRow.toDomain() = Favorite(teamId, name, country, logoUrl, leagueId, parseTimestamp(addedAt), alerts.toDomain())
fun Favorite.toRow() = FavoriteRow(teamId, name, country, logoUrl, leagueId, addedAt.toString(), alerts.toJson())
fun AlertsJson.toDomain() = AlertSettings(goalSound, redCardVibrate, penaltyVibrate, lineups)
fun AlertSettings.toJson() = AlertsJson(goalSound, redCardVibrate, penaltyVibrate, lineups)

@Serializable
data class TeamRow(
    @SerialName("team_id") val teamId: Int,
    val name: String,
    val code: String? = null,
    val country: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("venue_id") val venueId: Int? = null,
    @SerialName("updated_at") val updatedAt: String,
)

fun TeamRow.toDomain() = Team(teamId, name, logoUrl, code, country, venueId)
fun Team.toRow(now: Instant) = TeamRow(id, name, code, country, logoUrl, venueId, now.toString())

@Serializable
data class LeagueRow(
    val id: String,
    @SerialName("league_id") val leagueId: Int,
    val season: Int,
    val name: String,
    val country: String,
    @SerialName("logo_url") val logoUrl: String? = null,
    val coverage: CoverageJson,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class CoverageJson(
    val events: Boolean = false,
    val lineups: Boolean = false,
    val statisticsFixtures: Boolean = false,
    val statisticsPlayers: Boolean = false,
    val odds: Boolean = false,
)

fun leagueRowId(leagueId: Int, season: Int) = "${leagueId}_$season"

fun LeagueRow.toDomain() = League(
    leagueId, name, country, logoUrl, season,
    LeagueCoverage(coverage.events, coverage.lineups, coverage.statisticsFixtures, coverage.statisticsPlayers, coverage.odds),
)

fun League.toRow(now: Instant) = LeagueRow(
    leagueRowId(id, season), id, season, name, country, logoUrl,
    CoverageJson(coverage.events, coverage.lineups, coverage.statisticsFixtures, coverage.statisticsPlayers, coverage.odds),
    now.toString(),
)

@Serializable
data class FixturesDayRow(
    val id: String,
    @SerialName("league_id") val leagueId: Int,
    val day: String,                         // yyyy-MM-dd
    val fixtures: List<FixtureItemDto>,
    @SerialName("updated_at") val updatedAt: String,
)

fun fixturesDayRowId(leagueId: Int, day: String) = "${leagueId}_$day"

@Serializable
data class TopScorersRow(
    val id: String,                          // "{leagueId}_{season}"
    @SerialName("league_id") val leagueId: Int,
    val season: Int,
    val scorers: List<TopScorerItemDto>,     // raw /players/topscorers items
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class StandingsRow(
    val id: String,                          // "{leagueId}_{season}"
    @SerialName("league_id") val leagueId: Int,
    val season: Int,
    val standings: List<StandingsItemDto>,   // raw /standings items, mapped like a network response
    @SerialName("updated_at") val updatedAt: String,
)
