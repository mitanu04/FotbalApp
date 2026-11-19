package ro.fluierulfinal.data

import java.time.LocalDate

// Clasele de date ale aplicației. Le construim fie din răspunsurile API-Football (FixtureParser),
// fie din datele de exemplu din MatchRepository.

enum class MatchStatus { LIVE, FINISHED, UPCOMING }

enum class EventType { GOAL, YELLOW_CARD, RED_CARD }

data class League(
    val id: String,
    val name: String,
    val country: String,
    val abbr: String,   // prescurtarea afișată în pătrățelul colorat (ex. „SL”)
    val color: Long,    // culoare ARGB, ex. 0xFFC8102E
)

data class Team(
    val id: String,
    val name: String,
    val color: Long,
    val city: String = "",
    // coordonatele stadionului; null = încă nu le știm (în aplicație vin din /venues + Geocoder)
    val lat: Double? = null,
    val lng: Double? = null,
    // sigla echipei; implicit o construim din id pentru echipele cu id din API (ex. „api529” = Barcelona)
    val logoUrl: String? = apiLogoUrl(id),
)

/** Linkul siglei pe serverul de imagini API-Football, pentru id-urile de forma „api529”; altfel null. */
fun apiLogoUrl(teamId: String): String? =
    teamId.removePrefix("api").toIntOrNull()?.takeIf { teamId.startsWith("api") }
        ?.let { "https://media.api-sports.io/football/teams/$it.png" }

data class MatchEvent(
    val minute: Int,
    val type: EventType,
    val isHome: Boolean, // true = eveniment al echipei gazdă
    val player: String,
)

data class Match(
    val id: Int,
    val leagueId: String,
    val date: LocalDate,
    val kickoff: String,          // ora de start, ex. „20:30”
    val homeId: String,
    val awayId: String,
    val status: MatchStatus,
    val minute: Int? = null,      // doar pentru meciurile live
    val homeGoals: Int? = null,   // null cât timp meciul nu a început
    val awayGoals: Int? = null,
    val tvChannel: String,
    val events: List<MatchEvent> = emptyList(),
    val fromApi: Boolean = false, // true = meci real din API-Football, false = meci de exemplu
)

data class StatLine(val label: String, val home: Int, val away: Int)

enum class FormResult { WIN, DRAW, LOSS }

/** Un rezultat din forma unei echipe (ultimele 5 meciuri). */
data class FormEntry(
    val result: FormResult,
    val opponentId: String,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val atHome: Boolean,
    val date: LocalDate,
    val matchId: Int?,   // null = rezultat de exemplu, fără meci în aplicație
)

/** Un rând din planul de weekend: un slot (ex. „Sâmbătă seara”) și meciul ales pentru el. */
data class PlanSlot(
    val label: String,
    val match: Match?,          // null = slot liber
    val distanceKm: Int?,       // distanța până la stadion, dacă o știm
    val atStadium: Boolean,     // true = mergi la stadion, false = te uiți la TV
)
