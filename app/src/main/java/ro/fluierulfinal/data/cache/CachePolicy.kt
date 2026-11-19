package ro.fluierulfinal.data.cache

import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.FixtureStatus
import java.time.Duration
import java.time.Instant

/**
 * When a cached entry is still good enough to use instead of calling API-Football.
 * Pure functions (the current time is passed in), so they are easy to unit test.
 */
object CachePolicy {

    /** Teams and leagues change rarely (logo, name, coverage). */
    val STATIC_DATA_TTL: Duration = Duration.ofDays(7)

    /** A day's fixtures change (kick-off moved, scores), so they are refreshed more often. */
    val FIXTURES_TTL: Duration = Duration.ofHours(6)

    fun isStaticDataFresh(updatedAt: Instant, now: Instant): Boolean =
        !now.isAfter(updatedAt.plus(STATIC_DATA_TTL))

    /**
     * A league table: a finished past season never changes, so it never expires;
     * the current season's table follows the 6-hour fixtures rule.
     */
    fun isStandingsFresh(updatedAt: Instant, now: Instant, season: Int, currentSeason: Int): Boolean =
        season < currentSeason || !now.isAfter(updatedAt.plus(FIXTURES_TTL))

    /**
     * A day's fixtures are kept forever once every fixture is over (finished or cancelled):
     * nothing can change any more. Otherwise they expire after [FIXTURES_TTL].
     * An empty day ("no matches") follows the normal 6-hour rule, since fixtures can still be scheduled.
     */
    fun isFixturesDayFresh(updatedAt: Instant, now: Instant, fixtures: List<Fixture>): Boolean {
        val allOver = fixtures.isNotEmpty() &&
            fixtures.all { it.status == FixtureStatus.FINISHED || it.status == FixtureStatus.CANCELLED }
        return allOver || !now.isAfter(updatedAt.plus(FIXTURES_TTL))
    }
}
