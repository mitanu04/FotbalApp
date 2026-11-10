package ro.fluierulfinal.data

import java.time.LocalDate
import kotlin.random.Random

/**
 * Singurul loc din care ecranele primesc date.
 * Acum întoarce date de exemplu scrise de mână; mai târziu aici vom pune apelurile
 * către API-Football și cache-ul din Firestore, fără să schimbăm ecranele.
 */
class MatchRepository {

    // „Azi” e fixat ca datele de exemplu să arate la fel oricând rulezi aplicația.
    val today: LocalDate = LocalDate.of(2026, 10, 26)

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

    val teams: List<Team> = listOf(
        Team("rap", "Rapid", 0xFF8A1538, "București", 44.44, 26.11),
        Team("fcsb", "FCSB", 0xFFD4001F, "București", 44.44, 26.15),
        Team("din", "Dinamo", 0xFFD71920, "București", 44.45, 26.10),
        Team("ucv", "U. Craiova", 0xFF1460AA, "Craiova", 44.32, 23.80),
        Team("sep", "Sepsi OSK", 0xFFC4161C, "Sfântu Gheorghe", 45.86, 25.79),
        Team("cfr", "CFR Cluj", 0xFF7A1F2B, "Cluj-Napoca", 46.76, 23.57),
        Team("far", "Farul", 0xFF14427A, "Ovidiu", 44.26, 28.56),
        Team("poli", "Poli Timișoara", 0xFF5B2A86, "Timișoara", 45.74, 21.24),
        Team("meta", "Metaloglobus", 0xFF2E8B57, "București", 44.42, 26.17),
        Team("corv", "Corvinul", 0xFF1F5FA8, "Hunedoara", 45.75, 22.90),
        Team("bar", "Barcelona", 0xFFA50044, "Barcelona", 41.38, 2.12),
        Team("rso", "Real Sociedad", 0xFF0067B1, "San Sebastián", 43.30, -1.97),
        Team("sev", "Sevilla", 0xFFC8102E, "Sevilla", 37.38, -5.97),
        Team("osa", "Osasuna", 0xFF0A346F, "Pamplona", 42.80, -1.64),
        Team("rma", "Real Madrid", 0xFF6C5B1B, "Madrid", 40.45, -3.69),
        Team("get", "Getafe", 0xFF005999, "Getafe", 40.33, -3.71),
        Team("liv", "Liverpool", 0xFFC8102E, "Liverpool", 53.43, -2.96),
        Team("bre", "Brentford", 0xFFE30613, "Londra", 51.49, -0.29),
        Team("che", "Chelsea", 0xFF034694, "Londra", 51.48, -0.19),
        Team("ars", "Arsenal", 0xFFDB0007, "Londra", 51.55, -0.11),
        Team("mil", "AC Milan", 0xFFA2141B, "Milano", 45.48, 9.12),
        Team("bol", "Bologna", 0xFF1A2F48, "Bologna", 44.49, 11.31),
        Team("nap", "Napoli", 0xFF12A0D7, "Napoli", 40.83, 14.19),
        Team("laz", "Lazio", 0xFF3A8FC0, "Roma", 41.93, 12.45),
        Team("gen", "Genoa", 0xFF9E1B32, "Genova", 44.42, 8.95),
        Team("int", "Inter", 0xFF0068A8, "Milano", 45.48, 9.12),
        Team("bay", "Bayern", 0xFFDC052D, "München", 48.22, 11.62),
        Team("bvb", "Dortmund", 0xFFC9A200, "Dortmund", 51.49, 7.45),
        Team("rbl", "RB Leipzig", 0xFFDD0741, "Leipzig", 51.35, 12.35),
        Team("psg", "PSG", 0xFF004170, "Paris", 48.84, 2.25),
        Team("lyo", "Lyon", 0xFF14387F, "Lyon", 45.77, 4.98),
    )

    /** Locația utilizatorului. Acum e fixată în Timișoara; mai târziu o dă GPS-ul (Fused Location Provider). */
    val userLat = 45.7489
    val userLng = 21.2087

    val matches: List<Match> = buildSampleMatches()

    fun league(id: String): League = leagues.first { it.id == id }
    fun team(id: String): Team = teams.first { it.id == id }
    fun match(id: Int): Match = matches.first { it.id == id }

    /** Statistici de exemplu, mereu aceleași pentru același meci. Vor veni din /fixtures?id=X. */
    fun statsFor(match: Match): List<StatLine> {
        if (match.status == MatchStatus.UPCOMING) return emptyList()
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
        val rnd = Random(teamId.hashCode())
        val others = teams.filter { it.id != teamId }
        // rezultatele de exemplu le punem din săptămână în săptămână, înaintea celor reale
        val oldest = real.lastOrNull()?.date ?: today
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
        val d26 = today
        val d25 = today.minusDays(1)
        val d24 = today.minusDays(2)
        val d27 = today.plusDays(1)
        val d28 = today.plusDays(2)
        val fri = today.plusDays(4)
        val sat = today.plusDays(5)
        val sun = today.plusDays(6)
        fun goal(min: Int, home: Boolean, p: String) = MatchEvent(min, EventType.GOAL, home, p)
        fun yellow(min: Int, home: Boolean, p: String) = MatchEvent(min, EventType.YELLOW_CARD, home, p)
        fun red(min: Int, home: Boolean, p: String) = MatchEvent(min, EventType.RED_CARD, home, p)

        return listOf(
            Match(1, "sl", d26, "20:30", "rap", "fcsb", MatchStatus.LIVE, 34, 1, 0, "Digi Sport 1",
                listOf(goal(12, true, "Petrila"), yellow(27, false, "Olaru"))),
            Match(2, "sl", d26, "18:00", "sep", "ucv", MatchStatus.LIVE, 67, 0, 2, "Prima Sport 1",
                listOf(goal(22, false, "Bancu"), yellow(41, true, "Ștefan"), goal(58, false, "Baiaram"))),
            Match(3, "l2", d26, "15:00", "poli", "meta", MatchStatus.FINISHED, null, 2, 1, "Digi Sport 2",
                listOf(goal(9, true, "Bejan"), goal(44, false, "Rață"), red(71, false, "Pițian"), goal(80, true, "Dumitrescu"))),
            Match(4, "ll", d26, "22:00", "sev", "osa", MatchStatus.UPCOMING, tvChannel = "Digi Sport 3"),
            Match(5, "pl", d26, "22:00", "liv", "bre", MatchStatus.UPCOMING, tvChannel = "Prima Sport 2"),
            Match(6, "sa", d26, "21:45", "laz", "gen", MatchStatus.LIVE, 8, 0, 0, "Orange Sport 1"),
            Match(7, "ucl", d27, "22:00", "bar", "int", MatchStatus.UPCOMING, tvChannel = "Prima Sport 1"),
            Match(8, "ucl", d27, "22:00", "ars", "bay", MatchStatus.UPCOMING, tvChannel = "Prima Sport 2"),
            Match(9, "sa", d28, "21:45", "mil", "bol", MatchStatus.UPCOMING, tvChannel = "Digi Sport 1"),
            Match(10, "l1", d28, "22:00", "psg", "lyo", MatchStatus.UPCOMING, tvChannel = "Digi Sport 2"),
            Match(11, "ll", d25, "17:15", "bar", "rso", MatchStatus.FINISHED, null, 3, 1, "Digi Sport 1",
                listOf(goal(18, true, "Lewandowski"), goal(33, true, "Yamal"), goal(52, false, "Oyarzabal"),
                    yellow(64, false, "Zubimendi"), goal(88, true, "Raphinha"))),
            Match(12, "sl", d25, "20:00", "cfr", "far", MatchStatus.FINISHED, null, 1, 1, "Prima Sport 1",
                listOf(goal(30, false, "Alibec"), goal(76, true, "Bîrligea"))),
            Match(13, "pl", d25, "18:30", "che", "liv", MatchStatus.FINISHED, null, 2, 2, "Prima Sport 2",
                listOf(goal(11, false, "Salah"), goal(39, true, "Palmer"), goal(61, true, "Jackson"), goal(90, false, "Szoboszlai"))),
            Match(14, "sa", d25, "21:45", "nap", "mil", MatchStatus.FINISHED, null, 1, 2, "Digi Sport 3",
                listOf(goal(25, false, "Leão"), goal(49, true, "Lukaku"), red(83, true, "Anguissa"), goal(86, false, "Pulisic"))),
            Match(15, "l2", d24, "13:00", "corv", "poli", MatchStatus.FINISHED, null, 0, 0, "Digi Sport 2",
                listOf(yellow(55, false, "Bejan"))),
            Match(16, "ll", d24, "22:00", "rma", "get", MatchStatus.FINISHED, null, 2, 0, "Digi Sport 1",
                listOf(goal(37, true, "Mbappé"), goal(70, true, "Bellingham"))),
            // --- weekendul următor (30 oct – 1 nov), pentru planul de weekend ---
            Match(18, "l2", fri, "18:00", "poli", "corv", MatchStatus.UPCOMING, tvChannel = "Digi Sport 2"),
            Match(19, "sl", fri, "21:00", "ucv", "cfr", MatchStatus.UPCOMING, tvChannel = "Digi Sport 1"),
            Match(20, "ll", sat, "15:15", "bar", "sev", MatchStatus.UPCOMING, tvChannel = "Digi Sport 1"),
            Match(21, "pl", sat, "17:00", "liv", "ars", MatchStatus.UPCOMING, tvChannel = "Prima Sport 2"),
            Match(22, "sl", sat, "20:30", "din", "rap", MatchStatus.UPCOMING, tvChannel = "Prima Sport 1"),
            Match(23, "sa", sun, "13:30", "mil", "laz", MatchStatus.UPCOMING, tvChannel = "Digi Sport 3"),
            Match(24, "sl", sun, "21:00", "far", "fcsb", MatchStatus.UPCOMING, tvChannel = "Orange Sport 1"),
            Match(17, "bl", d24, "19:30", "bvb", "rbl", MatchStatus.FINISHED, null, 3, 3, "Prima Sport 3",
                listOf(goal(5, true, "Guirassy"), goal(20, false, "Openda"), goal(44, true, "Adeyemi"),
                    goal(58, false, "Šeško"), goal(77, false, "Simons"), goal(90, true, "Brandt"))),
        )
    }
}
