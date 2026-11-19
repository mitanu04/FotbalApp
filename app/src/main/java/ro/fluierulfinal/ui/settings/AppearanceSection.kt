package ro.fluierulfinal.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import ro.fluierulfinal.ui.theme.AppPalette
import ro.fluierulfinal.ui.theme.GazonDarkColors
import ro.fluierulfinal.ui.theme.Spacing
import ro.fluierulfinal.ui.theme.StadionDarkColors
import ro.fluierulfinal.ui.theme.ThemeController
import ro.fluierulfinal.ui.theme.ThemeMode
import javax.inject.Inject

/** Exposes the shared ThemeController to the Settings screen. */
@HiltViewModel
class AppearanceViewModel @Inject constructor(private val theme: ThemeController) : ViewModel() {
    val mode = theme.mode
    val palette = theme.palette
    fun setMode(mode: ThemeMode) = theme.setMode(mode)
    fun setPalette(palette: AppPalette) = theme.setPalette(palette)
}

/** "Aspect": choose the color palette (Gazon / Stadion) and light/dark (or follow the phone). Applies instantly. */
@Composable
fun AppearanceSection(viewModel: AppearanceViewModel = hiltViewModel()) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val palette by viewModel.palette.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    Text("Aspect", fontWeight = FontWeight.SemiBold, color = colors.primary, modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm))
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(colors.surface).padding(Spacing.lg),
    ) {
        Text("Paletă de culori", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
            AppPalette.entries.forEachIndexed { i, p ->
                SegmentedButton(
                    selected = palette == p,
                    onClick = { viewModel.setPalette(p) },
                    shape = SegmentedButtonDefaults.itemShape(i, AppPalette.entries.size),
                    icon = { PaletteSwatch(p) },
                    label = { Text(p.label) },
                )
            }
        }
        Text("Temă", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.padding(top = Spacing.lg))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
            val modes = listOf(ThemeMode.SYSTEM to "Sistem", ThemeMode.LIGHT to "Luminoasă", ThemeMode.DARK to "Întunecată")
            modes.forEachIndexed { i, (m, label) ->
                SegmentedButton(
                    selected = mode == m,
                    onClick = { viewModel.setMode(m) },
                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                    label = { Text(label, maxLines = 1) },
                )
            }
        }
    }
}

/** Two dots with the palette's dark background and accent, so the choice is visual. */
@Composable
private fun PaletteSwatch(palette: AppPalette) {
    val (bg, accent) = when (palette) {
        AppPalette.GAZON -> GazonDarkColors.background to GazonDarkColors.primary
        AppPalette.STADION -> StadionDarkColors.background to StadionDarkColors.primary
    }
    Box(Modifier.size(18.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
    }
}
