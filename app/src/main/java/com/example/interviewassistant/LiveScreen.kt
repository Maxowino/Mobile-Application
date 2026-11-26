package com.example.interviewassistant

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Animated bars shown while the AI is reading the question aloud. */
@Composable
private fun Waveform(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "wave")
    val heights = List(13) { i ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 380 + i * 53, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$i"
        )
    }
    Row(
        modifier = Modifier.height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { h ->
            val fraction = if (active) h.value else 0.15f
            Box(
                Modifier
                    .width(4.dp)
                    .height((40f * fraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(C.AccentText)
            )
        }
    }
}

@Composable
private fun MicButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .shadow(18.dp, CircleShape, ambientColor = C.Primary, spotColor = C.Primary)
            .clip(CircleShape)
            .background(if (enabled) PrimaryBrush else androidx.compose.ui.graphics.SolidColor(C.Disabled), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Txt("🎤", 28)
    }
}

@Composable
fun LiveScreen(state: InterviewState, speaker: Speaker) {
    val q = state.question ?: return
    val scope = rememberCoroutineScope()

    // true while the AI interviewer is reading the question aloud
    var speaking by remember(q.text) { mutableStateOf(true) }

    LaunchedEffect(q.text) {
        speaking = true
        speaker.speak(q.text) { speaking = false }
    }
    DisposableEffect(Unit) {
        onDispose { speaker.stop() }
    }

    // Speech-to-text: opens the system speech dialog and puts the recognised words in the answer box.
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val heard = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!heard.isNullOrBlank()) {
                state.answerText = (state.answerText + " " + heard).trim()
            }
        }
    }
    fun startListening() {
        speaker.stop()
        speaking = false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your answer")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            state.error = "Speech recognition is not available on this device. Type your answer instead."
        }
    }

    val status = when {
        speaking -> "AI SPEAKING"
        state.loading -> "SCORING YOUR ANSWER"
        else -> "YOUR TURN"
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        // header: LIVE INTERVIEW ...... Q1/5, then the progress bar
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(C.Green, CircleShape))
            Spacer(Modifier.width(8.dp))
            MonoLabel("LIVE INTERVIEW")
            Spacer(Modifier.weight(1f))
            MonoLabel("Q${q.number}/${q.total}")
        }
        Spacer(Modifier.height(10.dp))
        ProgressBar(q.number.toFloat() / q.total)

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            AvatarCircle(88.dp)
            Spacer(Modifier.height(4.dp))
            MonoLabel(status)
            Spacer(Modifier.height(14.dp))
            Waveform(active = speaking)
            Spacer(Modifier.height(22.dp))

            Box(Modifier.fillMaxWidth().card(radius = 20.dp, bg = C.Field, border = C.Border).padding(20.dp)) {
                Txt(
                    q.text, 17, FontWeight.SemiBold, align = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(), lineHeight = 26
                )
            }
            Spacer(Modifier.height(10.dp))
            MonoLabel("${q.difficulty.uppercase()} · ${q.category.uppercase()}", color = C.Hint)
            state.lastNote?.let {
                Spacer(Modifier.height(4.dp))
                MonoLabel(it, color = C.Hint)
            }

            if (!speaking) {
                Spacer(Modifier.height(24.dp))
                MicButton(enabled = !state.loading) { startListening() }
                Spacer(Modifier.height(8.dp))
                Txt("Tap the mic to answer out loud", 12, color = C.Muted)
                Spacer(Modifier.height(14.dp))
                AppTextField(
                    value = state.answerText,
                    onValueChange = { state.answerText = it },
                    placeholder = "Or type your answer here…",
                    singleLine = false,
                    minLines = 3,
                    enabled = !state.loading
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    "Submit answer",
                    onClick = { scope.launch { state.submit(state.answerText) } },
                    enabled = state.answerText.isNotBlank() && !state.loading
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        // bottom bar: Skip, then Back / Finish
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.Border))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(C.Disabled)
                    .clickable(enabled = !state.loading) {
                        if (speaking) {
                            speaker.stop()
                            speaking = false
                        } else {
                            // skipping a question = an empty answer, which scores 0
                            scope.launch { state.submit("") }
                        }
                    }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Txt(if (speaking) "Skip" else "Skip question", 12, color = C.Muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallButton("‹  Back", primary = false, onClick = { state.back() })
            Spacer(Modifier.weight(1f))
            SmallButton("Finish  ›", primary = true, onClick = {
                if (!state.loading) scope.launch { state.endEarly() }
            })
        }
    }
}
