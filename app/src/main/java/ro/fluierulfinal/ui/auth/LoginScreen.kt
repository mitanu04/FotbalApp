package ro.fluierulfinal.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ro.fluierulfinal.ui.components.PrimaryActionButton
import ro.fluierulfinal.ui.components.WhistleLogo

/** Login / Register on one screen; a link at the bottom switches between the two modes. */
@Composable
fun LoginScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val passwordTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()

    // Surface sets the default content color (onBackground) for every Text below; without it,
    // a Text with no explicit color falls back to black, invisible on the dark background.
    Surface(color = colors.background, contentColor = colors.onBackground, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(32.dp))
            WhistleLogo(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(colors.primaryContainer).padding(12.dp))
            Text(
                "Fluierul Final", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                if (state.isRegister) "Creează-ți un cont" else "Autentifică-te ca să-ți vezi echipele favorite",
                color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            OutlinedTextField(
                value = state.email, onValueChange = viewModel::onEmailChange,
                label = { Text("Email") }, singleLine = true, enabled = !state.loading,
                isError = state.fieldErrors.email != null,
                supportingText = state.fieldErrors.email?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password, onValueChange = viewModel::onPasswordChange,
                label = { Text("Parolă") }, singleLine = true, enabled = !state.loading,
                isError = state.fieldErrors.password != null,
                supportingText = state.fieldErrors.password?.let { { Text(it) } },
                visualTransformation = passwordTransformation,
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Ascunde" else "Arată") }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (state.isRegister) ImeAction.Next else ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); viewModel.submit() }),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (state.isRegister) {
                OutlinedTextField(
                    value = state.confirmPassword, onValueChange = viewModel::onConfirmPasswordChange,
                    label = { Text("Confirmă parola") }, singleLine = true, enabled = !state.loading,
                    isError = state.fieldErrors.confirmPassword != null,
                    supportingText = state.fieldErrors.confirmPassword?.let { { Text(it) } },
                    visualTransformation = passwordTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); viewModel.submit() }),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }

            // Server messages: an error (red) or an info such as "check your email" (accent color).
            state.error?.let { Message(it, colors.error, colors.errorContainer) }
            state.info?.let { Message(it, colors.onSecondaryContainer, colors.secondaryContainer) }

            // main action: gold in both themes (tertiary), see PrimaryActionButton
            val submitLabel = if (state.isRegister) "Creează cont" else "Intră în cont"
            PrimaryActionButton(
                text = submitLabel,
                onClick = { focus.clearFocus(); viewModel.submit() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(50.dp),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = colors.onTertiary, strokeWidth = 2.dp)
                } else {
                    Text(submitLabel, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
                }
            }

            TextButton(onClick = viewModel::toggleMode, enabled = !state.loading, modifier = Modifier.padding(top = 8.dp)) {
                Text(if (state.isRegister) "Ai deja cont? Autentifică-te" else "Nu ai cont? Creează unul")
            }
        }
    }
}

@Composable
private fun Message(text: String, color: androidx.compose.ui.graphics.Color, background: androidx.compose.ui.graphics.Color) {
    Text(
        text, color = color, fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(12.dp)).background(background).padding(12.dp),
    )
}
