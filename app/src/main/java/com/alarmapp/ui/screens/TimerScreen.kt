package com.alarmapp.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarmapp.service.FloatingTimerService

@Composable
fun TimerScreen() {
    val context = LocalContext.current
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }
    var isCountdown by remember { mutableStateOf(true) }
    var isServiceRunning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (isCountdown) "مؤقت تنازلي" else "مؤقت تصاعدي",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تنازلي", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = isCountdown, onCheckedChange = { isCountdown = it })
            Text("تصاعدي", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeInputField("ساعات", hours) { hours = it }
            TimeInputField("دقائق", minutes) { minutes = it }
            TimeInputField("ثوان", seconds) { seconds = it }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!isServiceRunning) {
            Button(
                onClick = {
                    val h = hours.toIntOrNull() ?: 0
                    val m = minutes.toIntOrNull() ?: 0
                    val s = seconds.toIntOrNull() ?: 0
                    val total = (h * 3600L) + (m * 60L) + s

                    if (total <= 0) {
                        errorMessage = "الرجاء إدخال وقت صحيح"
                        return@Button
                    }
                    errorMessage = ""
                    val intent = Intent(context, FloatingTimerService::class.java).apply {
                        putExtra("total_seconds", total)
                        putExtra("elapsed_seconds", 0L)
                        putExtra("is_countdown", isCountdown)
                    }
                    context.startForegroundService(intent)
                    isServiceRunning = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("تشغيل المؤقت العائم", fontSize = 18.sp)
            }
        } else {
            Button(
                onClick = {
                    context.stopService(Intent(context, FloatingTimerService::class.java))
                    isServiceRunning = false
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("إيقاف المؤقت", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "تعليمات:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• اضغط على المؤقت للبدء/الإيقاف")
                Text("• اسحب المؤقت لتحريكه في الشاشة")
                Text("• اسحب لأسفل (أكثر من 300 بكسل) لإلغائه")
                Text("• اضغط مطولاً (ثانيتين) لإلغائه")
            }
        }
    }
}

@Composable
fun TimeInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input: String ->
            val filtered = input.filter { it.isDigit() }
            if (filtered.length <= 2) onValueChange(filtered)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(90.dp),
        textAlign = TextAlign.Center,
        singleLine = true
    )
}
