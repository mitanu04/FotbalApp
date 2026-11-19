package ro.fluierulfinal.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.fluierulfinal.BuildConfig

/** One entry in a settings list: a title, an optional subtitle and a tag on the right (e.g. "S10"). */
data class SettingsLink(val title: String, val subtitle: String?, val tag: String?, val onClick: () -> Unit)

/**
 * Setări. In S8 it lists every planned screen (so the whole navigation can be demoed) and the
 * debug tools. The real settings (theme, GPS radius, plan day, simulation) arrive in S10/S12.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    plannedScreens: List<SettingsLink>,
    debugTools: List<SettingsLink>,
    accountSection: @Composable () -> Unit = {},
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Setări") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            accountSection()
            Section("Ecranele din planul aplicației", plannedScreens)
            Section("Dezvoltare", debugTools)
            Text(
                "Sursa datelor: " + if (BuildConfig.USE_FAKE_DATA) "date de test (USE_FAKE_DATA)" else "API-Football + Supabase",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun Section(title: String, links: List<SettingsLink>) {
    val colors = MaterialTheme.colorScheme
    Text(title, fontWeight = FontWeight.SemiBold, color = colors.primary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface)) {
        links.forEachIndexed { i, link ->
            if (i > 0) HorizontalDivider(color = colors.outline)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(onClick = link.onClick).padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(link.title, fontSize = 15.sp)
                    link.subtitle?.let { Text(it, fontSize = 12.sp, color = colors.onSurfaceVariant) }
                }
                link.tag?.let {
                    Text(
                        it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSecondaryContainer,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(colors.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

/** "Cont" section: who is signed in, and Deconectare (with a confirmation dialog). */
@Composable
fun AccountSection(email: String?, onSignOut: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var confirm by remember { mutableStateOf(false) }

    Text("Cont", fontWeight = FontWeight.SemiBold, color = colors.primary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Conectat ca", fontSize = 12.sp, color = colors.onSurfaceVariant)
            Text(email ?: "—", fontSize = 15.sp)
        }
        TextButton(onClick = { confirm = true }) { Text("Deconectare", color = colors.error) }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Te deconectezi?") },
            text = { Text("Echipele favorite rămân salvate în cont.") },
            confirmButton = { TextButton(onClick = { confirm = false; onSignOut() }) { Text("Deconectare") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Anulează") } },
        )
    }
}
