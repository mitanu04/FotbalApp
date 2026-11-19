package ro.fluierulfinal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ro.fluierulfinal.ui.debug.ApiCoverageScreen
import ro.fluierulfinal.ui.theme.FluierulFinalTheme
import ro.fluierulfinal.ui.theme.ThemeController
import javax.inject.Inject

/**
 * The only Activity. @AndroidEntryPoint lets Hilt inject fields such as [themeController].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeController: ThemeController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by themeController.mode.collectAsStateWithLifecycle()
            FluierulFinalTheme(mode) {
                // Temporary root: the API coverage check (step 3). Replaced by the
                // navigation graph with the bottom bar in step 5, where it moves under Setări.
                ApiCoverageScreen()
            }
        }
    }
}
