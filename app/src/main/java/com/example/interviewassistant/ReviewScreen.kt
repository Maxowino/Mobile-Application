package com.example.interviewassistant

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
private fun ScoreRing(score: Double, level: String, color: Color) {
    Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(
                color = C.RingTrack, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width = stroke)
            )
            drawArc(
                color = color, startAngle = -90f, sweepAngle = (360.0 * score / 100.0).toFloat(),
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Txt("${score.roundToInt()}", 46, FontWeight.Bold, color)
            Txt("/ 100", 12, color = C.Muted)
            Txt(level, 13, FontWeight.Bold)
        }
    }
}

@Composable
private fun ReviewItem(marker: String, markerColor: Color, text: String, bg: Color, border: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().card(radius = 14.dp, bg = bg, border = border).padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Txt(marker, 15, FontWeight.Bold, markerColor)
        Spacer(Modifier.width(12.dp))
        Txt(text, 13, color = C.Body, lineHeight = 20, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QuestionCard(index: Int, q: QuestionFeedback) {
    var expanded by remember { mutableStateOf(false) }
    val verdictColor = when (q.verdict) {
        "Strong" -> C.Green
        "Partly correct" -> C.Amber
        else -> C.Red
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card(radius = 14.dp)
            .clickable { expanded = !expanded }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Txt("$index.  ${q.question}", 13, FontWeight.Bold, lineHeight = 19, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Txt("${q.score.roundToInt()}", 18, FontWeight.Bold, verdictColor)
        }
        Txt("${q.verdict}  ·  ${q.difficulty}  ·  tap for details", 11, color = C.Muted)

        if (expanded) {
            Txt("Your answer: ${q.yourAnswer.ifBlank { "(none)" }}", 12, color = C.Body, lineHeight = 18)
            if (q.missingTerms.isNotEmpty()) {
                Txt("Missing key terms: " + q.missingTerms.joinToString(", "), 12, color = C.Amber, lineHeight = 18)
            }
            if (q.verdict != "Strong") {
                Txt("Model answer: ${q.modelAnswer}", 12, color = C.Body, lineHeight = 18)
            }
            if (q.studyNext.isNotEmpty()) {
                Txt("Study next: " + q.studyNext.joinToString("; "), 12, color = C.AccentText, lineHeight = 18)
            }
        }
    }
}

@Composable
fun ReviewScreen(state: InterviewState) {
    val rep = state.report ?: return
    val scope = rememberCoroutineScope()

    val levelColor = when (rep.level) {
        "Strong" -> C.Green
        "Developing" -> C.Amber
        else -> C.Red
    }
    val score = rep.averageScore.coerceIn(0.0, 100.0)

    val strengths = (rep.strongTopics.map { "Strong in $it" } +
        rep.questions.filter { it.verdict == "Strong" }.map { "Answered well: ${it.question}" }).take(4)
    val improvements = rep.studyPlan.take(4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLabel("INTERVIEW COMPLETE")
            Spacer(Modifier.weight(1f))
            Pill(rep.career)
        }
        Txt("Your Review", 28, FontWeight.Bold)

        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            ScoreRing(score, rep.level, levelColor)
        }
        Txt(rep.summary, 13, color = C.Muted, align = TextAlign.Center, lineHeight = 20, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))
        SectionTitle("Strengths", C.Green)
        if (strengths.isEmpty()) {
            ReviewItem("✓", C.Green, "No standout strengths yet. Keep practising.", C.GreenBg, C.GreenBorder)
        } else {
            strengths.forEach { ReviewItem("✓", C.Green, it, C.GreenBg, C.GreenBorder) }
        }

        Spacer(Modifier.height(8.dp))
        SectionTitle("Areas to Improve", C.Amber)
        if (improvements.isEmpty()) {
            ReviewItem("→", C.Amber, "Answer at least one question to get improvement tips.", C.AmberBg, C.AmberBorder)
        } else {
            improvements.forEach { ReviewItem("→", C.Amber, it, C.AmberBg, C.AmberBorder) }
        }

        Spacer(Modifier.height(8.dp))
        SectionTitle("Question Breakdown", C.AccentText)
        rep.questions.forEachIndexed { i, q -> QuestionCard(i + 1, q) }

        Spacer(Modifier.height(12.dp))
        PrimaryButton("Retry Interview", onClick = { scope.launch { state.start(state.career) } })
        SecondaryButton("Back to Home", onClick = { state.go(Screen.WELCOME) })
        Spacer(Modifier.height(8.dp))
    }
}
