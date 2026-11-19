package ro.fluierulfinal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ro.fluierulfinal.ui.detail.MatchDetailScreen
import ro.fluierulfinal.ui.home.HomeScreen
import ro.fluierulfinal.ui.home.HomeViewModel
import ro.fluierulfinal.ui.league.LeagueScreen
import ro.fluierulfinal.ui.theme.FluierulFinalTheme

class MainActivity : ComponentActivity() {

    // ViewModel-ul supraviețuiește rotirii ecranului, deci starea nu se pierde.
    private val vm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FluierulFinalTheme {
                val state by vm.state.collectAsState()
                // Navigare simplă: meci deschis → detalii; ligă deschisă → pagina ligii; altfel ecranul principal.
                // Mai târziu, când avem mai multe ecrane, trecem pe Navigation Compose.
                // Ordinea contează: un meci deschis din pagina ligii stă „deasupra” ei,
                // așa că „înapoi” din meci te întoarce în pagina ligii.
                val matchId = state.openMatchId
                val leagueId = state.openLeagueId
                when {
                    matchId != null -> {
                        BackHandler { vm.closeMatch() }
                        MatchDetailScreen(matchId, vm, onBack = vm::closeMatch)
                    }
                    leagueId != null -> {
                        BackHandler { vm.closeLeague() }
                        LeagueScreen(leagueId, vm, onBack = vm::closeLeague)
                    }
                    else -> HomeScreen(vm)
                }
            }
        }
    }
}
