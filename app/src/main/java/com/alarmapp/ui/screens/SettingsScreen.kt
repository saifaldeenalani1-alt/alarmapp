package com.alarmapp.ui.screens

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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("نغمة التنبيه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("يمكنك اختيار نغمة لكل منبه عند إضافته من شاشة المنبهات", style = MaterialTheme.typography.bodyMedium)
                }
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
