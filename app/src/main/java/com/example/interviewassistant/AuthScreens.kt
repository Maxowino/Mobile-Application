package com.example.interviewassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.Text




@Composable
private fun FieldIcon(kind: String) {
    val icon = when (kind) {
        "email" -> Icons.Outlined.Email
        "lock" -> Icons.Outlined.Lock
        else -> Icons.Outlined.Person
    }
    Icon(icon, null, modifier = Modifier.size(20.dp), tint = C.Muted)
}

@Composable
private fun EyeToggle(visible: Boolean, onToggle: () -> Unit) {
    Txt(if (visible) "🙈" else "👁", 16, modifier = Modifier.clickable(onClick = onToggle))
}

@Composable
private fun SocialButton(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(48.dp)
            .card(radius = 14.dp, bg = C.Field, border = C.Border)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Txt(icon, 15, FontWeight.Bold, if (label == "Google") Color(0xFF4285F4) else C.Text)
        Spacer(Modifier.width(8.dp))
        Txt(label, 14, FontWeight.Medium, C.Muted)
    }
}

@Composable
private fun CheckBox(checked: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(shape)
            .background(if (checked) C.Primary else C.Field, shape)
            .border(1.dp, if (checked) C.Primary else C.FieldBorder, shape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Txt("✓", 13, FontWeight.Bold, Color.White)
    }
}

@Composable
fun SignInScreen(state: InterviewState) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }


    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 90.dp)
        ) {
            Logo()
            Spacer(Modifier.height(36.dp))
            Txt("Welcome back", 30, FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Txt("Sign in to continue your interview practice", 14, color = C.Muted)
            Spacer(Modifier.height(28.dp))

            MonoLabel("EMAIL")
            Spacer(Modifier.height(8.dp))
            AppTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "you@example.com",
                leading = { FieldIcon("email") },
                keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(18.dp))

            MonoLabel("PASSWORD")
            Spacer(Modifier.height(8.dp))
            AppTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Enter your password",
                leading = { FieldIcon("lock") },
                trailing = { EyeToggle(showPassword) { showPassword = !showPassword } },
                password = !showPassword
            )
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Txt(
                    "Forgot password?", 12, color = C.AccentText,
                    modifier = Modifier.clickable { info = "Password reset is not available in this demo." }
                )
            }
            Spacer(Modifier.height(20.dp))

            PrimaryButton("Sign In", enabled = !state.loading, onClick = {
                val e = email.trim()
                if (!Patterns.EMAIL_ADDRESS.matcher(e).matches() || password.isBlank()) {
                    info = "Enter a valid email and your password."
                } else {
                    info = null
                    state.signIn(e, password)
                }
            })
            info?.let {
                Spacer(Modifier.height(12.dp))
                Txt(it, 12, color = C.Red)
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(C.Border))
                Txt("or continue with", 12, color = C.Muted, modifier = Modifier.padding(horizontal = 12.dp))
                Box(Modifier.weight(1f).height(1.dp).background(C.Border))
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SocialButton("G", "Google", Modifier.weight(1f)) {
                    info = "Google sign-in is not available in this demo."
                }
                SocialButton("🐙", "GitHub", Modifier.weight(1f)) {
                    info = "GitHub sign-in is not available in this demo."
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Txt("Don't have an account? ", 14, color = C.Muted)
            Txt(
                "Sign up", 14, FontWeight.Bold, C.AccentText,
                modifier = Modifier.clickable { state.go(Screen.SIGN_UP) }
            )
        }
    }
}

@Composable
fun SignUpScreen(state: InterviewState) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var agreed by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }


    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 90.dp)
        ) {
            Logo()
            Spacer(Modifier.height(28.dp))
            Txt("Create account", 30, FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Txt("Start practising interviews with AI today", 14, color = C.Muted)
            Spacer(Modifier.height(22.dp))

            MonoLabel("FULL NAME")
            Spacer(Modifier.height(8.dp))
            AppTextField(name, { name = it }, "Alex Johnson", leading = { FieldIcon("person") })
            Spacer(Modifier.height(16.dp))

            MonoLabel("EMAIL")
            Spacer(Modifier.height(8.dp))
            AppTextField(
                email, { email = it }, "you@example.com",
                leading = { FieldIcon("email") }, keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(16.dp))

            MonoLabel("PASSWORD")
            Spacer(Modifier.height(8.dp))
            AppTextField(
                password, { password = it }, "Min. 8 characters",
                leading = { FieldIcon("lock") },
                trailing = { EyeToggle(showPassword) { showPassword = !showPassword } },
                password = !showPassword
            )
            Spacer(Modifier.height(16.dp))

            MonoLabel("CONFIRM PASSWORD")
            Spacer(Modifier.height(8.dp))
            AppTextField(
                confirm, { confirm = it }, "Repeat your password",
                leading = { FieldIcon("lock") },
                trailing = { EyeToggle(showConfirm) { showConfirm = !showConfirm } },
                password = !showConfirm
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckBox(agreed) { agreed = !agreed }
                Spacer(Modifier.width(10.dp))
                Txt("I agree to the ", 12, color = C.Muted)
                Txt("Terms of Service", 12, color = C.AccentText)
                Txt(" and ", 12, color = C.Muted)
                Txt("Privacy Policy", 12, color = C.AccentText)
            }
            Spacer(Modifier.height(20.dp))

            PrimaryButton("Create Account", enabled = !state.loading, onClick = {
                val e = email.trim()
                info = when {
                    name.isBlank() -> "Enter your full name."
                    !Patterns.EMAIL_ADDRESS.matcher(e).matches() -> "Enter a valid email, like name@gmail.com."
                    password.length < 8 -> "Password must be at least 8 characters."
                    password != confirm -> "The passwords do not match."
                    !agreed -> "Please accept the Terms of Service and Privacy Policy."
                    else -> null
                }
                if (info == null) state.signUp(name.trim(), e, password)
            })
            info?.let {
                Spacer(Modifier.height(12.dp))
                Txt(it, 12, color = C.Red)
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Txt("Already have an account? ", 14, color = C.Muted)
            Txt(
                "Sign in", 14, FontWeight.Bold, C.AccentText,
                modifier = Modifier.clickable { state.go(Screen.SIGN_IN) }
            )
        }
    }
}
