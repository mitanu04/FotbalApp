package ro.fluierulfinal.data.cache

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.fluierulfinal.domain.model.Fixture
import ro.fluierulfinal.domain.model.FixtureStatus
import ro.fluierulfinal.domain.model.Score
import ro.fluierulfinal.domain.model.Team
import java.time.Duration
import java.time.Instant

class CachePolicyTest {

    private val written = Instant.parse("2026-11-19T10:00:00Z")

    private fun fixture(status: FixtureStatus) = Fixture(
        id = 1, leagueId = 283, season = 2026, kickoff = written, status = status, elapsedMinutes = null,
        homeTeam = Team(1, "A", null), awayTeam = Team(2, "B", null), score = Score(null, null),
    )

    @Test
    fun staticData_freshFor7Days() {
        assertTrue(CachePolicy.isStaticDataFresh(written, written.plus(Duration.ofDays(7))))
        assertFalse(CachePolicy.isStaticDataFresh(written, written.plus(Duration.ofDays(7)).plusSeconds(1)))
    }

    @Test
    fun fixturesDay_withUnfinishedMatches_expiresAfter6Hours() {
        val day = listOf(fixture(FixtureStatus.FINISHED), fixture(FixtureStatus.SCHEDULED))
        assertTrue(CachePolicy.isFixturesDayFresh(written, written.plus(Duration.ofHours(6)), day))
        assertFalse(CachePolicy.isFixturesDayFresh(written, written.plus(Duration.ofHours(6)).plusSeconds(1), day))
    }

    @Test
    fun fixturesDay_allFinishedOrCancelled_neverExpires() {
        val day = listOf(fixture(FixtureStatus.FINISHED), fixture(FixtureStatus.CANCELLED))
        assertTrue(CachePolicy.isFixturesDayFresh(written, written.plus(Duration.ofDays(365)), day))
    }

    @Test
    fun fixturesDay_liveOrPostponed_isNotPermanent() {
        assertFalse(CachePolicy.isFixturesDayFresh(written, written.plus(Duration.ofDays(1)), listOf(fixture(FixtureStatus.LIVE))))
        assertFalse(CachePolicy.isFixturesDayFresh(written, written.plus(Duration.ofDays(1)), listOf(fixture(FixtureStatus.POSTPONED))))
    }

    @Test
    fun emptyDay_followsTheNormal6HourRule() {
        assertTrue(CachePolicy.isFixturesDayFresh(written, written.plus(Duration.ofHours(5)), emptyList()))
        assertFalse(CachePolicy.isFixturesDayFresh(written, written.plus(Duration.ofHours(7)), emptyList()))
    }
}
