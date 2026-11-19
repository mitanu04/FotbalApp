package ro.fluierulfinal.domain.model

import java.time.Instant

/**
 * A team the user follows. Stored in the Supabase table `favorites` (one row per user and team).
 * Name and logo are copied in, so the Favorite list can be shown without calling the API.
 */
data class Favorite(
    val teamId: Int,
    val name: String,
    val country: String?,
    val logoUrl: String?,
    val leagueId: Int?,             // the team's main league, if known
    val addedAt: Instant,
    val alerts: AlertSettings = AlertSettings(),
)

/**
 * Per-team alert preferences, edited on the "Setări alerte" sheet in S8.
 * They are only *stored* in S8; the alerts themselves are delivered in S10.
 * All off by default: the user opts in.
 */
data class AlertSettings(
    val goalSound: Boolean = false,
    val redCardVibrate: Boolean = false,
    val penaltyVibrate: Boolean = false,
    val lineups: Boolean = false,
)
