package ro.fluierulfinal

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ro.fluierulfinal.navigation.AppRoot
import ro.fluierulfinal.ui.theme.FluierulFinalTheme
import ro.fluierulfinal.ui.theme.ThemeController
import ro.fluierulfinal.ui.theme.isDark
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
            val dark = mode.isDark()
            // Status/navigation bar icons must contrast with our theme, which (from S10) may differ
            // from the phone's own setting: light icons on dark theme, dark icons on light theme.
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            FluierulFinalTheme(mode) {
                AppRoot()
            }
        }
    }
}
