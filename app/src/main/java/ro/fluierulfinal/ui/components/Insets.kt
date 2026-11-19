package ro.fluierulfinal.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Height of the bottom navigation bar (including the system gesture bar under it), or 0 on screens
 * without it. Provided by AppNavHost. Tab screens draw edge-to-edge behind the bar and add this
 * as bottom contentPadding to their lists, so the last item can always scroll above the bar.
 */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }

/**
 * contentPadding for the list of a bottom-bar screen: the top comes from the screen's Scaffold
 * (top app bar), the bottom is the bottom bar plus [extraBottom] (e.g. room for a FAB).
 */
@Composable
fun tabListPadding(scaffoldPadding: PaddingValues, horizontal: Dp, extraBottom: Dp = 16.dp): PaddingValues = PaddingValues(
    start = horizontal,
    end = horizontal,
    top = scaffoldPadding.calculateTopPadding(),
    bottom = LocalBottomBarPadding.current + extraBottom,
)
