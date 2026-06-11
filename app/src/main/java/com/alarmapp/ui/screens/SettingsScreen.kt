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
                "الإعدادات",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تخصيص المؤقت العائم", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("تم نقل تخصيص حجم الخط ولون الخط ولون الخلفية والشفافية إلى شاشة المؤقت", style = MaterialTheme.typography.bodyMedium)
                }
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
