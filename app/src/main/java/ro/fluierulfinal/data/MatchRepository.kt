package ro.fluierulfinal.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ro.fluierulfinal.BuildConfig
import ro.fluierulfinal.data.api.ApiFootballClient
import ro.fluierulfinal.data.api.FixtureParser
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

/**
 * Singurul loc din care ecranele primesc date. Combină două surse:
 *  - API-Football, pentru ieri, azi și mâine (planul gratuit nu are acces la alte zile);
 *  - datele de exemplu scrise de mână, pentru restul zilelor.
 *
 * Datele din API sunt ținute în `mutableStateOf`: când se schimbă, Compose redesenează singur
 * ecranele care le-au citit, fără să le anunțăm noi.
 */
class MatchRepository(
    private val client: ApiFootballClient = ApiFootballClient(BuildConfig.API_FOOTBALL_KEY),
) {

    /** Azi, după ora României. */
    val today: LocalDate = LocalDate.now(ZONE)

    /** Zilele pentru care avem acces la API: ieri, azi, mâine. */
    val apiDays: List<LocalDate> = listOf(today.minusDays(1), today, today.plusDays(1))

    fun isApiDay(date: LocalDate) = date in apiDays

    /**
     * Datele de exemplu sunt construite în jurul unei zile de luni (ca în prima versiune, unde „azi” era luni),
     * mutată în săptămâna curentă, ca să cadă în jurul datei de azi și pe zilele potrivite ale săptămânii.
     */
    private val sampleAnchor: LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    val leagues: List<League> = listOf(
        League("sl", "SuperLiga", "România", "SL", 0xFFC8102E),
        League("l2", "Liga 2", "România", "L2", 0xFF1D4FA3),
        League("ll", "LaLiga", "Spania", "LL", 0xFFE8482C),
        League("pl", "Premier League", "Anglia", "PL", 0xFF4B1D6B),
        League("sa", "Serie A", "Italia", "SA", 0xFF0B6BCB),
        League("ucl", "Liga Campionilor", "Europa", "LC", 0xFF0E1E5B),
        League("bl", "Bundesliga", "Germania", "BL", 0xFFD20515),
        League("l1", "Ligue 1", "Franța", "L1", 0xFF1B2A3A),
    )

    private val sampleTeams: List<Team> = listOf(
        Team("api6231", "Rapid", 0xFF8A1538, "București", 44.44, 26.11),
        Team("api559", "FCSB", 0xFFD4001F, "București", 44.44, 26.15),
        Team("api635", "Dinamo", 0xFFD71920, "București", 44.45, 26.10),
        Team("api632", "U. Craiova", 0xFF1460AA, "Craiova", 44.32, 23.80),
        Team("api2585", "Sepsi OSK", 0xFFC4161C, "Sfântu Gheorghe", 45.86, 25.79),
        Team("api2246", "CFR Cluj", 0xFF7A1F2B, "Cluj-Napoca", 46.76, 23.57),
        Team("api2596", "Farul", 0xFF14427A, "Ovidiu", 44.26, 28.56),
        Team("api2606", "Poli Timișoara", 0xFF5B2A86, "Timișoara", 45.74, 21.24),
        Team("api2604", "Metaloglobus", 0xFF2E8B57, "București", 44.42, 26.17),
        Team("api20034", "Corvinul", 0xFF1F5FA8, "Hunedoara", 45.75, 22.90),
        Team("api529", "Barcelona", 0xFFA50044, "Barcelona", 41.38, 2.12),
        Team("api548", "Real Sociedad", 0xFF0067B1, "San Sebastián", 43.30, -1.97),
        Team("api536", "Sevilla", 0xFFC8102E, "Sevilla", 37.38, -5.97),
        Team("api727", "Osasuna", 0xFF0A346F, "Pamplona", 42.80, -1.64),
        Team("api541", "Real Madrid", 0xFF6C5B1B, "Madrid", 40.45, -3.69),
        Team("api546", "Getafe", 0xFF005999, "Getafe", 40.33, -3.71),
        Team("api40", "Liverpool", 0xFFC8102E, "Liverpool", 53.43, -2.96),
        Team("api55", "Brentford", 0xFFE30613, "Londra", 51.49, -0.29),
        Team("api49", "Chelsea", 0xFF034694, "Londra", 51.48, -0.19),
        Team("api42", "Arsenal", 0xFFDB0007, "Londra", 51.55, -0.11),
        Team("api489", "AC Milan", 0xFFA2141B, "Milano", 45.48, 9.12),
        Team("api500", "Bologna", 0xFF1A2F48, "Bologna", 44.49, 11.31),
        Team("api492", "Napoli", 0xFF12A0D7, "Napoli", 40.83, 14.19),
        Team("api487", "Lazio", 0xFF3A8FC0, "Roma", 41.93, 12.45),
        Team("api495", "Genoa", 0xFF9E1B32, "Genova", 44.42, 8.95),
        Team("api505", "Inter", 0xFF0068A8, "Milano", 45.48, 9.12),
        Team("api157", "Bayern", 0xFFDC052D, "München", 48.22, 11.62),
        Team("api165", "Dortmund", 0xFFC9A200, "Dortmund", 51.49, 7.45),
        Team("api173", "RB Leipzig", 0xFFDD0741, "Leipzig", 51.35, 12.35),
        Team("api85", "PSG", 0xFF004170, "Paris", 48.84, 2.25),
        Team("api80", "Lyon", 0xFF14387F, "Lyon", 45.77, 4.98),
    )

    /** Locația utilizatorului. Acum e fixată în Timișoara; mai târziu o dă GPS-ul (Fused Location Provider). */
    val userLat = 45.7489
    val userLng = 21.2087

    /**
     * Datele de exemplu au fost scrise pentru o zi „azi” fixă; după ce le mutăm în săptămâna curentă,
     * le potrivim starea cu data reală: în trecut = terminate, în viitor = neîncepute (fără scor).
     */
    private val sampleMatches: List<Match> = buildSampleMatches().map { m ->
        when {
            m.date < today && m.status == MatchStatus.LIVE -> m.copy(status = MatchStatus.FINISHED, minute = null)
            m.date > today && m.status != MatchStatus.UPCOMING ->
                m.copy(status = MatchStatus.UPCOMING, minute = null, homeGoals = null, awayGoals = null, events = emptyList())
            else -> m
        }
    }

    // --- ce am adus din API (stare observată de Compose) ---
    private var apiMatches by mutableStateOf<Map<LocalDate, List<Match>>>(emptyMap())
    private var apiTeams by mutableStateOf<Map<String, Team>>(emptyMap())
    private val loadedAt = mutableMapOf<LocalDate, Instant>()

    /** Toate meciurile: din API pentru zilele cu acces, de exemplu pentru restul. */
    val matches: List<Match>
        get() = sampleMatches.filter { !isApiDay(it.date) } + apiMatches.values.flatten()

    /** Echipele de exemplu (care au culori și coordonate alese de mână) au prioritate față de cele din API. */
    val teams: List<Team>
        get() = sampleTeams + apiTeams.values.filter { t -> sampleTeams.none { it.id == t.id } }

    fun isLoaded(date: LocalDate) = date in apiMatches

    /**
     * Aduce meciurile unei zile din API, dacă e o zi cu acces.
     * Fiecare zi se cere o singură dată; doar „azi” se cere din nou după [REFRESH_TODAY_SECONDS],
     * ca scorurile live să se actualizeze. Așa nu consumăm din cele 100 de cereri pe zi la fiecare apăsare.
     * Aruncă [ro.fluierulfinal.data.api.ApiException] dacă nu reușește.
     */
    suspend fun loadDay(date: LocalDate, force: Boolean = false) {
        if (!isApiDay(date)) return
        val last = loadedAt[date]
        val fresh = last != null &&
            (date != today || Instant.now().isBefore(last.plusSeconds(REFRESH_TODAY_SECONDS)))
        if (fresh && !force) return

        val json = client.fixturesOn(date)
        // conversia JSON → obiecte o facem pe un fir de calcul, nu pe cel al ecranului
        val parsed = withContext(Dispatchers.Default) { FixtureParser.parse(json) }
        apiTeams = apiTeams + parsed.teams.associateBy { it.id }
        apiMatches = apiMatches + (date to parsed.matches)
        loadedAt[date] = Instant.now()
    }

    fun league(id: String): League = leagues.first { it.id == id }
    fun team(id: String): Team = sampleTeams.firstOrNull { it.id == id } ?: apiTeams.getValue(id)
    fun match(id: Int): Match = matches.first { it.id == id }

    /** Statistici de exemplu, mereu aceleași pentru același meci. Vor veni din /fixtures?id=X. */
    fun statsFor(match: Match): List<StatLine> {
        // pentru meciurile reale nu inventăm statistici; le vom lua din /fixtures/statistics
        if (match.status == MatchStatus.UPCOMING || match.fromApi) return emptyList()
        val rnd = Random(match.id)
        // la un meci live, statisticile cresc odată cu minutul
        val progress = if (match.status == MatchStatus.LIVE) (match.minute ?: 0) / 90.0 else 1.0
        fun scaled(min: Int, max: Int) = ((min + rnd.nextInt(max - min + 1)) * progress).toInt()
        val possession = 38 + rnd.nextInt(25)
        val shotsHome = scaled(6, 18)
        val shotsAway = scaled(6, 18)
        return listOf(
            StatLine("Posesie (%)", possession, 100 - possession),
            StatLine("Șuturi", shotsHome, shotsAway),
            StatLine("Șuturi pe poartă",
                minOf(shotsHome, (match.homeGoals ?: 0) + rnd.nextInt(4)),
                minOf(shotsAway, (match.awayGoals ?: 0) + rnd.nextInt(4))),
            StatLine("Cornere", scaled(0, 9), scaled(0, 9)),
            StatLine("Faulturi", scaled(8, 16), scaled(8, 16)),
        )
    }

    /**
     * Forma unei echipe: ultimele 5 rezultate, cel mai recent primul.
     * Întâi meciurile terminate pe care le avem; restul sunt completate cu rezultate de exemplu.
     * În aplicație vor veni din /fixtures?team=X&last=5.
     */
    fun formFor(teamId: String): List<FormEntry> {
        val real = matches
            .filter { it.status == MatchStatus.FINISHED && (it.homeId == teamId || it.awayId == teamId) }
            .sortedWith(compareByDescending<Match> { it.date }.thenByDescending { it.kickoff })
            .map { m ->
                val home = m.homeId == teamId
                val gf = if (home) m.homeGoals!! else m.awayGoals!!
                val ga = if (home) m.awayGoals!! else m.homeGoals!!
                FormEntry(resultOf(gf, ga), if (home) m.awayId else m.homeId, gf, ga, home, m.date, m.id)
            }
        // completăm cu rezultate inventate doar echipele din datele de exemplu, nu pe cele reale
        if (sampleTeams.none { it.id == teamId }) return real.take(5)
        val rnd = Random(teamId.hashCode())
        val others = sampleTeams.filter { it.id != teamId }
        // rezultatele de exemplu le punem din săptămână în săptămână, înaintea celor reale
        val oldest = real.lastOrNull()?.date ?: sampleAnchor
        val filler = List(5) { k ->
            val gf = rnd.nextInt(4)
            val ga = rnd.nextInt(3)
            FormEntry(resultOf(gf, ga), others[rnd.nextInt(others.size)].id, gf, ga,
                atHome = rnd.nextBoolean(), date = oldest.minusDays(7L * (k + 1)), matchId = null)
        }
        return (real + filler).take(5)
    }

    /** Următorul meci al echipei (live sau programat), pentru pătrățelul „?” din formă. */
    fun nextMatchFor(teamId: String): Match? = matches
        .filter { it.status != MatchStatus.FINISHED && (it.homeId == teamId || it.awayId == teamId) }
        .minWithOrNull(compareBy({ it.date }, { it.kickoff }))

    private fun resultOf(gf: Int, ga: Int) = when {
        gf > ga -> FormResult.WIN
        gf == ga -> FormResult.DRAW
        else -> FormResult.LOSS
    }

    private fun buildSampleMatches(): List<Match> {
        val d26 = sampleAnchor
        val d25 = sampleAnchor.minusDays(1)
        val d24 = sampleAnchor.minusDays(2)
        val d27 = sampleAnchor.plusDays(1)
        val d28 = sampleAnchor.plusDays(2)
        val fri = sampleAnchor.plusDays(4)
        val sat = sampleAnchor.plusDays(5)
        val sun = sampleAnchor.plusDays(6)
        fun goal(min: Int, home: Boolean, p: String) = MatchEvent(min, EventType.GOAL, home, p)
        fun yellow(min: Int, home: Boolean, p: String) = MatchEvent(min, EventType.YELLOW_CARD, home, p)
        fun red(min: Int, home: Boolean, p: String) = MatchEvent(min, EventType.RED_CARD, home, p)

        return listOf(
            Match(1, "sl", d26, "20:30", "api6231", "api559", MatchStatus.LIVE, 34, 1, 0, "Digi Sport 1",
                listOf(goal(12, true, "Petrila"), yellow(27, false, "Olaru"))),
            Match(2, "sl", d26, "18:00", "api2585", "api632", MatchStatus.LIVE, 67, 0, 2, "Prima Sport 1",
                listOf(goal(22, false, "Bancu"), yellow(41, true, "Ștefan"), goal(58, false, "Baiaram"))),
            Match(3, "l2", d26, "15:00", "api2606", "api2604", MatchStatus.FINISHED, null, 2, 1, "Digi Sport 2",
                listOf(goal(9, true, "Bejan"), goal(44, false, "Rață"), red(71, false, "Pițian"), goal(80, true, "Dumitrescu"))),
            Match(4, "ll", d26, "22:00", "api536", "api727", MatchStatus.UPCOMING, tvChannel = "Digi Sport 3"),
            Match(5, "pl", d26, "22:00", "api40", "api55", MatchStatus.UPCOMING, tvChannel = "Prima Sport 2"),
            Match(6, "sa", d26, "21:45", "api487", "api495", MatchStatus.LIVE, 8, 0, 0, "Orange Sport 1"),
            Match(7, "ucl", d27, "22:00", "api529", "api505", MatchStatus.UPCOMING, tvChannel = "Prima Sport 1"),
            Match(8, "ucl", d27, "22:00", "api42", "api157", MatchStatus.UPCOMING, tvChannel = "Prima Sport 2"),
            Match(9, "sa", d28, "21:45", "api489", "api500", MatchStatus.UPCOMING, tvChannel = "Digi Sport 1"),
            Match(10, "l1", d28, "22:00", "api85", "api80", MatchStatus.UPCOMING, tvChannel = "Digi Sport 2"),
            Match(11, "ll", d25, "17:15", "api529", "api548", MatchStatus.FINISHED, null, 3, 1, "Digi Sport 1",
                listOf(goal(18, true, "Lewandowski"), goal(33, true, "Yamal"), goal(52, false, "Oyarzabal"),
                    yellow(64, false, "Zubimendi"), goal(88, true, "Raphinha"))),
            Match(12, "sl", d25, "20:00", "api2246", "api2596", MatchStatus.FINISHED, null, 1, 1, "Prima Sport 1",
                listOf(goal(30, false, "Alibec"), goal(76, true, "Bîrligea"))),
            Match(13, "pl", d25, "18:30", "api49", "api40", MatchStatus.FINISHED, null, 2, 2, "Prima Sport 2",
                listOf(goal(11, false, "Salah"), goal(39, true, "Palmer"), goal(61, true, "Jackson"), goal(90, false, "Szoboszlai"))),
            Match(14, "sa", d25, "21:45", "api492", "api489", MatchStatus.FINISHED, null, 1, 2, "Digi Sport 3",
                listOf(goal(25, false, "Leão"), goal(49, true, "Lukaku"), red(83, true, "Anguissa"), goal(86, false, "Pulisic"))),
            Match(15, "l2", d24, "13:00", "api20034", "api2606", MatchStatus.FINISHED, null, 0, 0, "Digi Sport 2",
                listOf(yellow(55, false, "Bejan"))),
            Match(16, "ll", d24, "22:00", "api541", "api546", MatchStatus.FINISHED, null, 2, 0, "Digi Sport 1",
                listOf(goal(37, true, "Mbappé"), goal(70, true, "Bellingham"))),
            // --- weekendul următor (30 oct – 1 nov), pentru planul de weekend ---
            Match(18, "l2", fri, "18:00", "api2606", "api20034", MatchStatus.UPCOMING, tvChannel = "Digi Sport 2"),
            Match(19, "sl", fri, "21:00", "api632", "api2246", MatchStatus.UPCOMING, tvChannel = "Digi Sport 1"),
            Match(20, "ll", sat, "15:15", "api529", "api536", MatchStatus.UPCOMING, tvChannel = "Digi Sport 1"),
            Match(21, "pl", sat, "17:00", "api40", "api42", MatchStatus.UPCOMING, tvChannel = "Prima Sport 2"),
            Match(22, "sl", sat, "20:30", "api635", "api6231", MatchStatus.UPCOMING, tvChannel = "Prima Sport 1"),
            Match(23, "sa", sun, "13:30", "api489", "api487", MatchStatus.UPCOMING, tvChannel = "Digi Sport 3"),
            Match(24, "sl", sun, "21:00", "api2596", "api559", MatchStatus.UPCOMING, tvChannel = "Orange Sport 1"),
            Match(17, "bl", d24, "19:30", "api165", "api173", MatchStatus.FINISHED, null, 3, 3, "Prima Sport 3",
                listOf(goal(5, true, "Guirassy"), goal(20, false, "Openda"), goal(44, true, "Adeyemi"),
                    goal(58, false, "Šeško"), goal(77, false, "Simons"), goal(90, true, "Brandt"))),
        )
    }

    companion object {
        val ZONE: ZoneId = ZoneId.of("Europe/Bucharest")
        const val REFRESH_TODAY_SECONDS = 120L
    }
}
