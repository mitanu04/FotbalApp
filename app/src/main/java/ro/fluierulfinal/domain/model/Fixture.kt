package ro.fluierulfinal.domain.model

import java.time.Instant

/** One match. */
data class Fixture(
    val id: Int,
    val leagueId: Int,
    val season: Int,
    val kickoff: Instant,            // start time as an exact moment; screens convert it to local time
    val status: FixtureStatus,
    val elapsedMinutes: Int?,        // only while live, e.g. 67
    val homeTeam: Team,
    val awayTeam: Team,
    val score: Score,
    val venueName: String? = null,
    val venueCity: String? = null,
)

/** Goals; both null before kick-off. */
data class Score(val home: Int?, val away: Int?)

/**
 * Our simplified match status. The Explorează screen groups fixtures by it:
 * Live / Programate (SCHEDULED + POSTPONED) / Terminate (FINISHED + CANCELLED).
 */
enum class FixtureStatus {
    SCHEDULED,
    LIVE,
    FINISHED,
    POSTPONED,
    CANCELLED,
}
