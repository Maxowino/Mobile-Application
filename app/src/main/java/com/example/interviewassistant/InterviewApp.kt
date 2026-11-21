package com.example.interviewassistant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun InterviewApp(state: InterviewState) {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    DisposableEffect(Unit) {
        onDispose { speaker.shutdown() }
    }
    var showServerDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = state.canGoBack) { state.back() }

    AppTheme {
        AppBackground {
            Box(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                when (state.screen) {
                    Screen.SIGN_IN -> SignInScreen(state)
                    Screen.SIGN_UP -> SignUpScreen(state)
                    Screen.WELCOME -> WelcomeScreen(state, onServerClick = { showServerDialog = true })
                    Screen.CAREER -> CareerScreen(state)
                    Screen.CV -> CvScreen(state)
                    Screen.READY -> ReadyScreen(state)
                    Screen.LIVE -> LiveScreen(state, speaker)
                    Screen.REVIEW -> ReviewScreen(state)
                }

                StatusBanner(state, Modifier.align(Alignment.TopCenter))

                if (showServerDialog) {
                    ServerDialog(state) { showServerDialog = false }
                }
            }
        }
    }
}

/** Loading spinner and error message, shown at the top of any screen. */
@Composable
private fun StatusBanner(state: InterviewState, modifier: Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (state.loading) {
            Row(
                modifier = Modifier.card(radius = 20.dp, bg = C.Field, border = C.Border).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = C.Primary)
                Spacer(Modifier.width(10.dp))
                Txt("Working…", 12, color = C.Muted)
            }
        }
        state.error?.let { message ->
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .card(radius = 14.dp, bg = C.AmberBg, border = C.Red)
                    .clickable { state.error = null }
                    .padding(12.dp)
            ) {
                Txt("$message  (tap to dismiss)", 12, color = C.Red, lineHeight = 18)
            }
        }
    }
}

/** Lets you point the app at the Flask server (the design has no field for it). */
@Composable
private fun ServerDialog(state: InterviewState, onClose: () -> Unit) {
    var text by remember { mutableStateOf(state.baseUrl) }
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = C.Card,
        title = { Txt("Server address", 18, FontWeight.Bold) },
        text = {
            Column {
                Txt(
                    "Emulator: http://10.0.2.2:5000\nPhone on the same Wi-Fi: http://<your PC's IP>:5000",
                    12, color = C.Muted, lineHeight = 18
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(text, { text = it }, "http://10.0.2.2:5000")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                state.setServer(text)
                onClose()
            }) { Text("Save", color = C.AccentText) }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel", color = C.Muted) }
        }
    )
}
