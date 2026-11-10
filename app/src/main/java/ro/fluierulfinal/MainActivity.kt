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
                // Navigare simplă: dacă e un meci deschis, arătăm detaliile; altfel ecranul principal.
                // Mai târziu, când avem mai multe ecrane, trecem pe Navigation Compose.
                val openId = state.openMatchId
                if (openId != null) {
                    BackHandler { vm.closeMatch() }
                    MatchDetailScreen(openId, vm, onBack = vm::closeMatch)
                } else {
                    HomeScreen(vm)
                }
            }
        }
    }
}
