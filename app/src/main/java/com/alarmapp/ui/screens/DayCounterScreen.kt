package com.alarmapp.ui.screens

import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alarmapp.data.PreferencesManager
import com.alarmapp.model.DayCounterEvent
import com.alarmapp.util.formatDateFull
import java.util.Calendar

@Composable
fun DayCounterScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var events by remember { mutableStateOf(prefs.getEvents()) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun refresh() { events = prefs.getEvents() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (events.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("لا توجد مناسبات", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("اضغط على + لإضافة مناسبة جديدة", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onDelete = {
                            prefs.deleteEvent(event.id)
                            refresh()
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "إضافة مناسبة")
        }
    }

    if (showAddDialog) {
        AddEventDialog(
            onDismiss = { showAddDialog = false },
            onSave = { event ->
                prefs.saveEvent(event)
                refresh()
                showAddDialog = false
                val ids = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(android.content.ComponentName(context, com.alarmapp.widget.DayCounterWidget::class.java))
                if (ids.isNotEmpty()) {
                    val updateIntent = android.content.Intent(context, com.alarmapp.widget.DayCounterWidget::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(updateIntent)
                }
            }
        )
    }
}

@Composable
fun EventCard(event: DayCounterEvent, onDelete: () -> Unit) {
    val now = Calendar.getInstance()
    val eventCal = Calendar.getInstance().apply { timeInMillis = event.date }
    val diffDays: Long

    if (event.isCountdown) {
        diffDays = (eventCal.timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)
    } else {
        diffDays = (now.timeInMillis - eventCal.timeInMillis) / (1000 * 60 * 60 * 24)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDateFull(event.date),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (event.isCountdown) "متبقي" else "مضى",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (diffDays < 0) "0" else diffDays.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "يوم",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddEventDialog(onDismiss: () -> Unit, onSave: (DayCounterEvent) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var isCountdown by remember { mutableStateOf(true) }
    var dateText by remember { mutableStateOf(formatDateFull(System.currentTimeMillis())) }
    var widgetTextColor by remember { mutableStateOf(0xFFFFFFFF.toInt()) }
    var widgetBgColor by remember { mutableStateOf(0xCC000000.toInt()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مناسبة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المناسبة") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val c = Calendar.getInstance()
                                c.set(year, month, day, 0, 0, 0)
                                selectedDateMillis = c.timeInMillis
                                dateText = formatDateFull(selectedDateMillis)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(dateText)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("تنازلي (متبقي)", modifier = Modifier.weight(1f))
                    Switch(checked = isCountdown, onCheckedChange = { isCountdown = it })
                    Text("تصاعدي (مضى)")
                }

                Text("تخصيص الـ widget", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("لون الخط", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "أبيض" to 0xFFFFFFFF.toInt(), "أحمر" to 0xFFD93030.toInt(),
                        "أخضر" to 0xFF34A853.toInt(), "أزرق" to 0xFF1A73E8.toInt(),
                        "أسود" to 0xFF000000.toInt()
                    ).forEach { (label, color) ->
                        ColorButton(color = color, label = label, isSelected = widgetTextColor == color, onClick = { widgetTextColor = color })
                    }
                }

                Text("لون الخلفية", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "أسود" to 0xCC000000.toInt(), "أزرق" to 0xCC1A73E8.toInt(),
                        "أخضر" to 0xCC34A853.toInt(), "أحمر" to 0xCCD93030.toInt(),
                        "رمادي" to 0xCC444444.toInt()
                    ).forEach { (label, color) ->
                        ColorButton(color = color, label = label, isSelected = widgetBgColor == color, onClick = { widgetBgColor = color })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    onSave(
                        DayCounterEvent(
                            name = name,
                            date = selectedDateMillis,
                            isCountdown = isCountdown,
                            widgetTextColor = widgetTextColor,
                            widgetBgColor = widgetBgColor
                        )
                    )
                }
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
