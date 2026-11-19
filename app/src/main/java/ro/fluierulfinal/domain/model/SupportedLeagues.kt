package ro.fluierulfinal.domain.model

/**
 * The leagues the app supports. IDs checked against /leagues on 19.11.2026 (API coverage check).
 * Logos come straight from the API-Football media server, which does not count against the quota.
 */
object SupportedLeagues {

    const val CURRENT_SEASON = 2026

    /**
     * Season shown in the league tables. The free API-Football plan refuses /standings for 2026
     * ("try from 2022 to 2024"), so the app shows the last complete season it may access: 2024/25.
     * With a paid plan, set this to CURRENT_SEASON.
     */
    const val STANDINGS_SEASON = 2024

    val all: List<League> = listOf(
        league(283, "Liga I", "România"),
        league(284, "Liga II", "România"),
        league(39, "Premier League", "Anglia"),
        league(140, "La Liga", "Spania"),
        league(135, "Serie A", "Italia"),
        league(78, "Bundesliga", "Germania"),
        league(61, "Ligue 1", "Franța"),
        league(2, "Liga Campionilor", "Europa"),
    )

    val ids: Set<Int> = all.map { it.id }.toSet()

    fun logoUrl(leagueId: Int) = "https://media.api-sports.io/football/leagues/$leagueId.png"

    private fun league(id: Int, name: String, country: String) =
        League(id = id, name = name, country = country, logoUrl = logoUrl(id), season = CURRENT_SEASON)
}
