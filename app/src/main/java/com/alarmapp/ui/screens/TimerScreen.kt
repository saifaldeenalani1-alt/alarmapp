package com.alarmapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarmapp.service.FloatingTimerService
import com.alarmapp.util.formatTimeShort
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen() {
    val context = LocalContext.current

    val hourOptions = (0..23).map { it.toString().padStart(2, '0') }
    val minSecOptions = (0..59).map { it.toString().padStart(2, '0') }

    var selectedHours by remember { mutableStateOf("00") }
    var selectedMinutes by remember { mutableStateOf("01") }
    var selectedSeconds by remember { mutableStateOf("00") }
    var isCountdown by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    var fontSize by remember { mutableStateOf(24) }
    var fontColor by remember { mutableStateOf(0xFFFFFFFF.toInt()) }
    var bgColor by remember { mutableStateOf(0xCC000000.toInt()) }
    var bgTransparency by remember { mutableStateOf(80) }

    var activeTimers by remember { mutableStateOf<List<TimerInfo>>(emptyList()) }

    fun refreshActiveTimers() {
        val ids = FloatingTimerService.getActiveTimerIds()
        activeTimers = ids.map { id ->
            val pair = FloatingTimerService.getTimerInfo(id)
            TimerInfo(id, pair?.first ?: 0L, pair?.second ?: false)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshActiveTimers()
            kotlinx.coroutines.delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("مؤقت عائم", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isCountdown,
                    onClick = { isCountdown = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("تنازلي") }
                SegmentedButton(
                    selected = !isCountdown,
                    onClick = { isCountdown = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("تصاعدي") }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DropdownTimeSelector("ساعات", hourOptions, selectedHours) { selectedHours = it }
                DropdownTimeSelector("دقائق", minSecOptions, selectedMinutes) { selectedMinutes = it }
                DropdownTimeSelector("ثوان", minSecOptions, selectedSeconds) { selectedSeconds = it }
            }
        }

        if (errorMessage.isNotEmpty()) {
            item {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تخصيص الشكل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    Text("حجم الخط", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(18 to "صغير", 24 to "متوسط", 30 to "كبير", 36 to "كبير جداً", 48 to "ضخم").forEach { (size, label) ->
                            FilterChip(
                                selected = fontSize == size,
                                onClick = { fontSize = size },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("لون الخط", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        colorOptions.forEach { (label, color) ->
                            ColorButton(color = color, label = label, isSelected = fontColor == color, onClick = { fontColor = color })
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("لون الخلفية", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        bgColorOptions.forEach { (label, color) ->
                            ColorButton(color = color, label = label, isSelected = bgColor == color, onClick = { bgColor = color })
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("شفافية الخلفية", style = MaterialTheme.typography.bodyMedium)
                    Text("${bgTransparency}%", style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = bgTransparency.toFloat(),
                        onValueChange = { bgTransparency = it.toInt() },
                        valueRange = 10f..100f,
                        steps = 8
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("معاينة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(bgColor).copy(alpha = bgTransparency / 100f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatTimeShort(selectedHours.toInt() * 3600L + selectedMinutes.toInt() * 60L + selectedSeconds.toInt()),
                            fontSize = fontSize.sp,
                            color = Color(fontColor),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val total = (selectedHours.toInt() * 3600L) + (selectedMinutes.toInt() * 60L) + selectedSeconds.toInt()
                    if (total <= 0) {
                        errorMessage = "الرجاء إدخال وقت صحيح"
                        return@Button
                    }
                    errorMessage = ""
                    val timerId = UUID.randomUUID().toString()
                    FloatingTimerService.start(
                        context, timerId, total, isCountdown,
                        fontSize, fontColor, bgColor, bgTransparency
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("تشغيل مؤقت عائم جديد", fontSize = 18.sp)
            }
        }

        if (activeTimers.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("المؤقتات النشطة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(activeTimers, key = { it.id }) { timer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (timer.isRunning) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (!timer.isRunning) CardDefaults.outlinedCardBorder() else null
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                formatTimeShort(timer.displaySeconds),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (timer.isRunning) "يعمل" else "متوقف",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = {
                            FloatingTimerService.remove(context, timer.id)
                        }) {
                            Icon(Icons.Default.Close, "إيقاف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { FloatingTimerService.stopAll(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("إيقاف جميع المؤقتات")
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تعليمات:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("• اضغط على المؤقت للبدء/الإيقاف")
                    Text("• اسحب المؤقت لتحريكه")
                    Text("• اضغط مطولاً لحذف المؤقت")
                    Text("• يمكن تشغيل أكثر من مؤقت في نفس الوقت")
                }
            }
        }
    }
}

private val colorOptions = listOf(
    "أبيض" to 0xFFFFFFFF.toInt(), "أحمر" to 0xFFD93030.toInt(),
    "أخضر" to 0xFF34A853.toInt(), "أزرق" to 0xFF1A73E8.toInt(),
    "أسود" to 0xFF000000.toInt(), "أصفر" to 0xFFFFD600.toInt(),
    "برتقالي" to 0xFFFF6D00.toInt(), "بنفسجي" to 0xFFAA00FF.toInt(),
    "وردي" to 0xFFFF4081.toInt(), "سماوي" to 0xFF00BCD4.toInt()
)

private val bgColorOptions = listOf(
    "أسود" to 0xCC000000.toInt(), "أزرق" to 0xCC1A73E8.toInt(),
    "أخضر" to 0xCC34A853.toInt(), "أحمر" to 0xCCD93030.toInt(),
    "رمادي" to 0xCC444444.toInt(), "أصفر" to 0xCCFFD600.toInt(),
    "برتقالي" to 0xCCFF6D00.toInt(), "بنفسجي" to 0xCCAA00FF.toInt(),
    "وردي" to 0xCCFF4081.toInt(), "سماوي" to 0xCC00BCD4.toInt()
)

data class TimerInfo(val id: String, val displaySeconds: Long, val isRunning: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownTimeSelector(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .width(100.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
