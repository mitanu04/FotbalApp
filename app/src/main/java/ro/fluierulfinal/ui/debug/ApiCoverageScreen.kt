package ro.fluierulfinal.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Debug screen for the one-off API coverage check. Not part of the normal user flow. */
@Composable
fun ApiCoverageScreen(viewModel: ApiCoverageViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    Column(
        Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Test acoperire API", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
        Text(
            "Apelează o singură dată /status (gratuit), /leagues?country=Romania, /leagues?id=140 " +
                "și un test /fixtures cu sezon. Cost: 3 cereri din 100.",
            fontSize = 13.sp, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = viewModel::run, enabled = !state.running) {
                Text(if (state.report == null) "Rulează testul" else "Rulează din nou (3 cereri)")
            }
            if (state.running) CircularProgressIndicator(Modifier.padding(start = 12.dp).size(24.dp))
        }

        state.error?.let {
            Text("Eroare: $it", color = colors.error, modifier = Modifier.padding(top = 12.dp))
        }
        state.report?.let {
            Text(
                it, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = colors.onSurface,
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(colors.surface).padding(12.dp),
            )
        }
    }
}
