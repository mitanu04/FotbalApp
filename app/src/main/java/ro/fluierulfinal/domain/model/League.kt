package ro.fluierulfinal.domain.model

/**
 * A league (or cup) for one season. In API-Football the season is the year the season *starts*:
 * the 2026/27 Premier League is season 2026.
 */
data class League(
    val id: Int,
    val name: String,
    val country: String,
    val logoUrl: String?,
    val season: Int,
    val coverage: LeagueCoverage = LeagueCoverage(),
)

/**
 * What data API-Football has for this league and season. Some screens in S10 depend on it
 * (no `events` → no goal timeline, no `lineups` → no lineup alerts), so we check before calling.
 */
data class LeagueCoverage(
    val events: Boolean = false,
    val lineups: Boolean = false,
    val statisticsFixtures: Boolean = false,
    val statisticsPlayers: Boolean = false,
    val odds: Boolean = false,
)
