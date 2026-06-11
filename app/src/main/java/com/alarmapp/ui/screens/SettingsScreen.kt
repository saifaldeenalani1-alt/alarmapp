package com.alarmapp.ui.screens

import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alarmapp.data.PreferencesManager
import com.alarmapp.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var settings by remember { mutableStateOf(prefs.getSettings()) }

    fun save(s: AppSettings) {
        settings = s
        prefs.saveSettings(s)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "التخصيص",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Text("حجم الخط", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val sizes = listOf("صغير" to 12, "متوسط" to 16, "كبير" to 20, "كبير جداً" to 24)
                sizes.forEach { (label, size) ->
                    FilterChip(
                        selected = settings.fontSize == size,
                        onClick = { save(settings.copy(fontSize = size)) },
                        label = { Text(label) }
                    )
                }
            }
        }

        item {
            Text("لون الخط", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val colors = listOf(
                    "أبيض" to 0xFFFFFFFF.toInt(),
                    "أحمر" to 0xFFD93030.toInt(),
                    "أخضر" to 0xFF34A853.toInt(),
                    "أزرق" to 0xFF1A73E8.toInt(),
                    "أسود" to 0xFF000000.toInt()
                )
                colors.forEach { (label, color) ->
                    ColorButton(
                        color = color,
                        label = label,
                        isSelected = settings.fontColor == color,
                        onClick = { save(settings.copy(fontColor = color)) }
                    )
                }
            }
        }

        item {
            Text("لون الخلفية", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val bgColors = listOf(
                    "أسود" to 0xCC000000.toInt(),
                    "أزرق" to 0xCC1A73E8.toInt(),
                    "أخضر" to 0xCC34A853.toInt(),
                    "أحمر" to 0xCCD93030.toInt(),
                    "رمادي" to 0xCC444444.toInt()
                )
                bgColors.forEach { (label, color) ->
                    ColorButton(
                        color = color,
                        label = label,
                        isSelected = settings.backgroundColor == color,
                        onClick = { save(settings.copy(backgroundColor = color)) }
                    )
                }
            }
        }

        item {
            Text("الشفافية", style = MaterialTheme.typography.titleMedium)
            Text("${settings.transparency}%", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = settings.transparency.toFloat(),
                onValueChange = { save(settings.copy(transparency = it.toInt())) },
                valueRange = 10f..100f,
                steps = 8
            )
        }

        item {
            Text("نغمة التنبيه", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر نغمة التنبيه")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            if (settings.alarmToneUri.isNotEmpty()) Uri.parse(settings.alarmToneUri) else null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("اختيار نغمة التنبيه")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الاهتزاز عند التنبيه", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = settings.vibrateEnabled,
                    onCheckedChange = { save(settings.copy(vibrateEnabled = it)) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("المؤقت العائم", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = settings.floatingTimerEnabled,
                    onCheckedChange = { save(settings.copy(floatingTimerEnabled = it)) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { save(AppSettings()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إعادة الإعدادات الافتراضية")
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "حول التطبيق",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("تطبيق المنبه والمؤقت المتكامل")
            Text("الإصدار 1.0")
            Text("جميع الحقوق محفوظة © 2026")
        }
    }
}

@Composable
fun ColorButton(color: Int, label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Surface(
                modifier = Modifier.size(16.dp),
                shape = MaterialTheme.shapes.small,
                color = Color(color)
            ) {}
        }
    )
}
