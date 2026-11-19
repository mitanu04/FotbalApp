package ro.fluierulfinal.domain.model

/**
 * Something that happened in a match (goal, card, substitution...).
 * Only the model for now: it is filled from /fixtures/events in S10 (match details, alerts).
 */
data class MatchEvent(
    val fixtureId: Int,
    val minute: Int,
    val extraMinute: Int?,          // stoppage time, e.g. 90+3 → minute 90, extraMinute 3
    val type: MatchEventType,
    val teamId: Int,
    val playerName: String?,
    val assistName: String?,        // for goals: the assist; for substitutions: the player coming in
)

enum class MatchEventType {
    GOAL,
    OWN_GOAL,
    PENALTY_GOAL,
    MISSED_PENALTY,
    YELLOW_CARD,
    RED_CARD,
    SUBSTITUTION,
    VAR,
    OTHER,
}
