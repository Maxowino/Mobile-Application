package com.example.interviewassistant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ChangePasswordDialog(onClose: () -> Unit, onChanged: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        containerColor = C.Card,
        title = { Txt("Change password", 18, FontWeight.Bold) },
        text = {
            Column {
                AppTextField(current, { current = it }, "Current password", password = true, enabled = !busy)
                Spacer(Modifier.height(10.dp))
                AppTextField(newPass, { newPass = it }, "New password (min. 8 characters)", password = true, enabled = !busy)
                Spacer(Modifier.height(10.dp))
                AppTextField(confirm, { confirm = it }, "Repeat new password", password = true, enabled = !busy)
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Txt(it, 12, color = C.Red, lineHeight = 18)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    error = when {
                        current.isBlank() -> "Enter your current password."
                        newPass.length < 8 -> "The new password must be at least 8 characters."
                        newPass != confirm -> "The new passwords do not match."
                        newPass == current -> "The new password must be different from the current one."
                        else -> null
                    }
                    if (error == null) {
                        busy = true
                        Repo.changePassword(current, newPass) { message ->
                            busy = false
                            if (message == null) {
                                onChanged()
                                onClose()
                            } else {
                                error = message
                            }
                        }
                    }
                }
            ) { Text(if (busy) "Saving…" else "Update", color = C.AccentText) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onClose) { Text("Cancel", color = C.Muted) }
        }
    )
}