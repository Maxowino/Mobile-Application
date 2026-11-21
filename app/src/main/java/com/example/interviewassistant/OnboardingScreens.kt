package com.example.interviewassistant

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

fun careerEmoji(name: String): String = when (name) {
    "Software Engineering" -> "💻"
    "Web Development" -> "🌐"
    "Systems and DevOps" -> "☁️"
    "Data and AI" -> "📊"
    "Cybersecurity" -> "🔐"
    else -> "💼"
}

// ------------------------------------------------------------------------------------------
// Welcome
// ------------------------------------------------------------------------------------------
@Composable
private fun FeatureChip(text: String) {
    Box(Modifier.card(radius = 20.dp, bg = C.Field, border = C.Border).padding(horizontal = 12.dp, vertical = 7.dp)) {
        Txt(text, 12, color = C.Muted)
    }
}

@Composable
fun WelcomeScreen(state: InterviewState, onServerClick: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AvatarCircle(90.dp)
            Spacer(Modifier.height(12.dp))
            BrandName(34)
            Spacer(Modifier.height(12.dp))
            Txt(
                "Your AI-powered interview coach with real-time voice feedback", 14,
                color = C.Muted, align = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureChip("🎤 Voice Interview")
                FeatureChip("📄 CV Scanner")
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureChip("🧠 Adaptive AI")
                FeatureChip("📊 Review")
            }
            Spacer(Modifier.height(36.dp))
            PrimaryButton("Start Your Interview", onClick = { state.go(Screen.CAREER) })
        }
        MonoLabel(
            "SERVER  ${state.baseUrl}",
            color = C.Hint,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .clickable(onClick = onServerClick)
        )
    }
}

// ------------------------------------------------------------------------------------------
// Choose career
// ------------------------------------------------------------------------------------------
@Composable
private fun CareerCard(c: Career, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .card(
                radius = 18.dp,
                bg = if (selected) C.CardSelected else C.Card,
                border = if (selected) C.PrimaryBorder else C.Border
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Txt(careerEmoji(c.name), 26)
        Spacer(Modifier.height(14.dp))
        Txt(c.name, 13, FontWeight.Bold, if (selected) C.AccentText else C.Text)
        Spacer(Modifier.height(4.dp))
        Txt("$QUESTIONS_PER_INTERVIEW questions", 12, color = C.Muted)
    }
}

@Composable
fun CareerScreen(state: InterviewState) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        if (state.careers.isEmpty()) state.loadCareers()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        BackRow { state.back() }
        Spacer(Modifier.height(8.dp))
        Txt("Choose Your Career Path", 24, FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Txt("We'll tailor questions to your specialization", 13, color = C.Muted)
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.careers.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { c ->
                        CareerCard(c, state.selectedCareer == c.name, Modifier.weight(1f)) {
                            state.selectedCareer = c.name
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (state.careers.isEmpty() && !state.loading) {
                SecondaryButton("Reload careers", onClick = { scope.launch { state.loadCareers() } })
            }
        }

        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            "Continue",
            onClick = { state.go(Screen.CV) },
            enabled = state.selectedCareer != null
        )
    }
}

// ------------------------------------------------------------------------------------------
// Upload CV
// ------------------------------------------------------------------------------------------
private fun displayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
    }
    return uri.lastPathSegment ?: "CV selected"
}

@Composable
fun CvScreen(state: InterviewState) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // The file name is shown; sending the CV to the server is the next step (needs a /cv endpoint).
        if (uri != null) state.cvName = displayName(context, uri)
    }
    val dashColor = C.FieldBorder

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        BackRow { state.back() }
        Spacer(Modifier.height(8.dp))
        Txt("Upload Your CV", 24, FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Txt("We'll scan it to personalize interview questions for", 13, color = C.Muted)
        Txt(state.selectedCareer ?: "", 13, color = C.AccentText)
        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .drawBehind {
                    drawRoundRect(
                        color = dashColor,
                        cornerRadius = CornerRadius(20.dp.toPx()),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                        )
                    )
                }
                .clickable {
                    picker.launch(
                        arrayOf(
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "text/plain"
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(56.dp).card(radius = 16.dp, bg = C.Disabled, border = C.Disabled),
                    contentAlignment = Alignment.Center
                ) { Txt(if (state.cvName == null) "📄" else "✅", 24) }
                Spacer(Modifier.height(14.dp))
                Txt(
                    state.cvName ?: "Tap to upload CV", 14, FontWeight.Bold,
                    align = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(4.dp))
                Txt(
                    if (state.cvName == null) "PDF, DOC, DOCX, TXT" else "Tap to change", 12,
                    color = C.Muted
                )
            }
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(
            if (state.cvName == null) "Skip — Start Without CV" else "Continue",
            onClick = { state.go(Screen.READY) }
        )
        Spacer(Modifier.height(12.dp))
        Txt(
            "Skip for now", 13, color = C.Muted, align = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    state.cvName = null
                    state.go(Screen.READY)
                }
                .padding(vertical = 8.dp)
        )
    }
}

// ------------------------------------------------------------------------------------------
// You're all set
// ------------------------------------------------------------------------------------------
@Composable
private fun FeatureRow(icon: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().card(radius = 14.dp).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Txt(icon, 22)
        Spacer(Modifier.width(14.dp))
        Column {
            Txt(title, 14, FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Txt(description, 12, color = C.Muted)
        }
    }
}

@Composable
fun ReadyScreen(state: InterviewState) {
    val scope = rememberCoroutineScope()
    val careerName = state.selectedCareer ?: ""

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        BackRow { state.back() }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().card(radius = 16.dp).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).card(radius = 12.dp, bg = C.CardSelected, border = C.CardSelected),
                    contentAlignment = Alignment.Center
                ) { Txt(careerEmoji(careerName), 22) }
                Spacer(Modifier.width(14.dp))
                Column {
                    MonoLabel("INTERVIEW SESSION")
                    Spacer(Modifier.height(2.dp))
                    Txt(careerName, 16, FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Txt("You're all set", 24, FontWeight.Bold)
            Txt(
                "Your AI interviewer will ask adaptive questions and follow up based on your answers. " +
                    "You'll get a full review at the end.",
                14, color = C.Muted, lineHeight = 22
            )
            Spacer(Modifier.height(4.dp))
            FeatureRow("🎤", "Use your voice", "Tap the mic to answer out loud — just like a real interview")
            FeatureRow("👂", "Listen carefully", "The AI interviewer will speak the questions aloud")
            FeatureRow("🧠", "Adaptive follow-ups", "Questions adapt to what you say in real time")
            FeatureRow("📊", "Detailed review", "Get scored and receive specific improvement tips")
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            "Begin Interview",
            onClick = { scope.launch { state.start(careerName) } },
            enabled = careerName.isNotEmpty() && !state.loading
        )
    }
}
