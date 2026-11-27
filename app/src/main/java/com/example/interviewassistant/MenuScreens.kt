package com.example.interviewassistant

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private fun levelColor(level: String): Color = when (level) {
    "Strong" -> C.Green
    "Developing" -> C.Amber
    else -> C.Red
}

// Profile

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier.card(radius = 16.dp).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Txt(value, 22, FontWeight.Bold, C.AccentText)
        Spacer(Modifier.height(2.dp))
        Txt(label, 11, color = C.Muted, align = TextAlign.Center)
    }
}

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<HistoryItem>?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Repo.loadProfile { name = it }
        Repo.loadHistory { list, _ -> items = list ?: emptyList() }
    }

    val done = items ?: emptyList()
    val average = if (done.isEmpty()) "–" else done.map { it.score }.average().roundToInt().toString()
    val best = if (done.isEmpty()) "–" else done.maxOf { it.score }.roundToInt().toString()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        AvatarCircle(90.dp)
        Spacer(Modifier.height(10.dp))
        Txt(if (name.isBlank()) "Your profile" else name, 22, FontWeight.Bold)
        Txt(Repo.email, 13, color = C.Muted)
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Interviews", done.size.toString(), Modifier.weight(1f))
            StatCard("Average score", average, Modifier.weight(1f))
            StatCard("Best score", best, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))

        Column(Modifier.fillMaxWidth()) {
            MonoLabel("FULL NAME")
            Spacer(Modifier.height(8.dp))
            AppTextField(name, { name = it }, "Alex Johnson")
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Save profile", onClick = {
            if (name.isBlank()) {
                info = "Enter your name."
            } else {
                Repo.saveProfile(name.trim()) { ok -> info = if (ok) "Profile saved." else "Could not save. Check your connection." }
            }
        })
        info?.let {
            Spacer(Modifier.height(12.dp))
            Txt(it, 12, color = C.Muted)
        }

        Spacer(Modifier.height(24.dp))
        MonoLabel("SECURITY")
        Spacer(Modifier.height(8.dp))
        SecondaryButton("Change password", onClick = { showPassword = true })
        Spacer(Modifier.height(16.dp))
    }

    if (showPassword) {
        ChangePasswordDialog(
            onClose = { showPassword = false },
            onChanged = {
                android.widget.Toast.makeText(context, "Password updated", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
}


// Interview history

private val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())

@Composable
fun HistoryScreen(onOpen: (String) -> Unit) {
    var items by remember { mutableStateOf<List<HistoryItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        Repo.loadHistory { list, err ->
            items = list
            error = err
        }
    }

    Box(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        val list = items
        when {
            error != null -> Txt(error ?: "", 13, color = C.Red, modifier = Modifier.align(Alignment.Center))
            list == null -> Txt("Loading…", 14, color = C.Muted, modifier = Modifier.align(Alignment.Center))
            list.isEmpty() -> Txt(
                "No interviews yet.\nFinish an interview and it will appear here.", 14,
                color = C.Muted, align = TextAlign.Center, modifier = Modifier.align(Alignment.Center)
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .card(radius = 16.dp)
                            .clickable { onOpen(item.id) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Txt(careerEmoji(item.career), 24)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Txt(item.career, 14, FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Txt(dateFormat.format(Date(item.date)), 12, color = C.Muted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Txt("${item.score.roundToInt()}", 20, FontWeight.Bold, levelColor(item.level))
                            Txt(item.level, 11, color = C.Muted)
                        }
                    }
                }
            }
        }
    }
}


// Settings
@Composable
private fun SpeedChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .card(radius = 14.dp, bg = if (selected) C.CardSelected else C.Field, border = if (selected) C.PrimaryBorder else C.Border)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Txt(label, 13, FontWeight.Medium, if (selected) C.AccentText else C.Muted)
    }
}

@Composable
fun SettingsScreen(state: InterviewState) {
    val context = LocalContext.current
    var readAloud by remember { mutableStateOf(AppPrefs.readAloud(context)) }
    var speed by remember { mutableStateOf(AppPrefs.speed(context)) }
    var showServer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionTitle("Interview voice", C.AccentText)

        Row(
            modifier = Modifier.fillMaxWidth().card(radius = 16.dp).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Txt("Read questions aloud", 14, FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Txt("The AI interviewer speaks each question", 12, color = C.Muted)
            }
            Switch(
                checked = readAloud,
                onCheckedChange = {
                    readAloud = it
                    AppPrefs.setReadAloud(context, it)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = C.Primary)
            )
        }

        Column(Modifier.fillMaxWidth().card(radius = 16.dp).padding(14.dp)) {
            Txt("Speech speed", 14, FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("slow" to "Slow", "normal" to "Normal", "fast" to "Fast").forEach { (key, label) ->
                    SpeedChip(label, speed == key, Modifier.weight(1f)) {
                        speed = key
                        AppPrefs.setSpeed(context, key)
                    }
                }
            }
        }

        SectionTitle("Connection", C.AccentText)
        Row(
            modifier = Modifier.fillMaxWidth().card(radius = 16.dp).clickable { showServer = true }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Txt("Server address", 14, FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Txt(state.baseUrl, 12, color = C.Muted)
            }
            Txt("›", 22, color = C.Muted)
        }
    }
    if (showServer) ServerDialog(state) { showServer = false }
}