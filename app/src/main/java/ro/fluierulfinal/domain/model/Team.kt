package ro.fluierulfinal.domain.model

/**
 * A football team. [id] is the API-Football team id (e.g. 529 = Barcelona),
 * used everywhere as the team's identity: in Firestore documents, favorites and fixtures.
 */
data class Team(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val code: String? = null,     // short code, e.g. "BAR"; not every team has one
    val country: String? = null,
    val venueId: Int? = null,     // home stadium id in API-Football, used later for "Meciuri în zonă"
)
