package ro.fluierulfinal.data.remote.mapper

import ro.fluierulfinal.data.remote.dto.FixtureItemDto
import ro.fluierulfinal.data.remote.dto.LeagueItemDto
import ro.fluierulfinal.data.remote.dto.StandingRecordDto
import ro.fluierulfinal.data.remote.dto.StandingRowDto
import ro.fluierulfinal.data.remote.dto.StandingsItemDto
import ro.fluierulfinal.data.remote.dto.TeamItemDto
import ro.fluierulfinal.data.remote.dto.TopScorerItemDto
import ro.fluierulfinal.data.remote.dto.TeamRefDto
import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.FixtureStatus
import ro.fluierulfinal.domain.model.FormLetter
import ro.fluierulfinal.domain.model.League
import ro.fluierulfinal.domain.model.LeagueCoverage
import ro.fluierulfinal.domain.model.Score
import ro.fluierulfinal.domain.model.StandingGroup
import ro.fluierulfinal.domain.model.StandingRecord
import ro.fluierulfinal.domain.model.StandingRow
import ro.fluierulfinal.domain.model.Standings
import ro.fluierulfinal.domain.model.Team
import ro.fluierulfinal.domain.model.TopScorer
import java.time.Instant

// DTO → domain mappers. The rest of the app only sees domain models, so if API-Football
// changes a field name, only the DTOs and these functions have to change.

fun FixtureItemDto.toDomain(): Fixture = Fixture(
    id = fixture.id,
    leagueId = league.id,
    season = league.season,
    kickoff = Instant.ofEpochSecond(fixture.timestamp),
    status = fixtureStatusOf(fixture.status.short),
    elapsedMinutes = fixture.status.elapsed?.takeIf { fixtureStatusOf(fixture.status.short) == FixtureStatus.LIVE },
    homeTeam = teams.home.toDomain(),
    awayTeam = teams.away.toDomain(),
    score = Score(goals.home, goals.away),
    venueName = fixture.venue?.name,
    venueCity = fixture.venue?.city,
)

fun TeamRefDto.toDomain(): Team = Team(id = id, name = name, logoUrl = logo)

fun TeamItemDto.toDomain(): Team = Team(
    id = team.id,
    name = team.name,
    logoUrl = team.logo,
    code = team.code,
    country = team.country,
    venueId = venue?.id,
)

/**
 * A /leagues item lists every season; we keep the one asked for,
 * or the season the API marks as current when [season] is null.
 * Returns null if that season is not in the list.
 */
fun LeagueItemDto.toDomain(season: Int? = null): League? {
    val s = if (season != null) seasons.firstOrNull { it.year == season } else seasons.firstOrNull { it.current }
    s ?: return null
    return League(
        id = league.id,
        name = league.name,
        country = country.name,
        logoUrl = league.logo,
        season = s.year,
        coverage = LeagueCoverage(
            events = s.coverage.fixtures.events,
            lineups = s.coverage.fixtures.lineups,
            statisticsFixtures = s.coverage.fixtures.statisticsFixtures,
            statisticsPlayers = s.coverage.fixtures.statisticsPlayers,
            odds = s.coverage.odds,
        ),
    )
}

/**
 * API-Football short status codes (https://www.api-football.com/documentation-v3#tag/Fixtures)
 * mapped to our five statuses. Unknown codes count as SCHEDULED.
 */
fun fixtureStatusOf(short: String): FixtureStatus = when (short) {
    "1H", "HT", "2H", "ET", "BT", "P", "SUSP", "INT", "LIVE" -> FixtureStatus.LIVE
    "FT", "AET", "PEN", "AWD", "WO" -> FixtureStatus.FINISHED
    "PST" -> FixtureStatus.POSTPONED
    "CANC", "ABD" -> FixtureStatus.CANCELLED
    else -> FixtureStatus.SCHEDULED       // "TBD", "NS"
}

/**
 * /standings item → domain. Group names lose the league prefix ("Liga I: Championship Round" →
 * "Championship Round"); the form string is reversed, because the API lists the most recent result last.
 */
fun StandingsItemDto.toDomain(): Standings = Standings(
    leagueId = league.id,
    season = league.season,
    groups = league.standings.filter { it.isNotEmpty() }.map { rows ->
        StandingGroup(
            name = rows.first().group?.substringAfter(": ")?.trim().orEmpty(),
            rows = rows.sortedBy { it.rank }.map { it.toDomain() },
        )
    },
)

fun StandingRowDto.toDomain(): StandingRow = StandingRow(
    rank = rank,
    team = team.toDomain(),
    played = all.played,
    win = all.win,
    draw = all.draw,
    lose = all.lose,
    goalsFor = all.goals.`for` ?: 0,
    goalsAgainst = all.goals.against ?: 0,
    goalDiff = goalsDiff,
    points = points,
    form = form.orEmpty().reversed().mapNotNull {
        when (it) {
            'W' -> FormLetter.WIN
            'D' -> FormLetter.DRAW
            'L' -> FormLetter.LOSS
            else -> null
        }
    },
    description = description?.takeIf { it.isNotBlank() },
    home = home.toDomain(),
    away = away.toDomain(),
)

fun StandingRecordDto.toDomain() = StandingRecord(played, win, draw, lose, goals.`for` ?: 0, goals.against ?: 0)

/** /players/topscorers → domain, in the API's order (most goals first); ties share the same rank. */
fun List<TopScorerItemDto>.toTopScorers(): List<TopScorer> {
    val rows = mapNotNull { item ->
        val st = item.statistics.firstOrNull() ?: return@mapNotNull null
        TopScorer(
            rank = 0, playerId = item.player.id, name = item.player.name, photoUrl = item.player.photo,
            team = st.team.toDomain(), goals = st.goals.total ?: 0, assists = st.goals.assists ?: 0,
            penalties = st.penalty.scored ?: 0, appearances = st.games.appearences ?: 0,
        )
    }
    return rows.mapIndexed { i, r -> r.copy(rank = rows.indexOfFirst { it.goals == r.goals }.takeIf { it >= 0 }?.plus(1) ?: (i + 1)) }
}
