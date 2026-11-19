package ro.fluierulfinal.data.remote.mapper

import ro.fluierulfinal.data.remote.dto.FixtureItemDto
import ro.fluierulfinal.data.remote.dto.LeagueItemDto
import ro.fluierulfinal.data.remote.dto.TeamItemDto
import ro.fluierulfinal.data.remote.dto.TeamRefDto
import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.FixtureStatus
import ro.fluierulfinal.domain.model.League
import ro.fluierulfinal.domain.model.LeagueCoverage
import ro.fluierulfinal.domain.model.Score
import ro.fluierulfinal.domain.model.Team
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
