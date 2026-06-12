package com.alarmapp.ui.screens

import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alarmapp.data.PreferencesManager
import com.alarmapp.model.DayCounterEvent
import com.alarmapp.ui.components.ColorPickerGrid
import com.alarmapp.util.formatDateFull
import java.util.Calendar

@Composable
fun DayCounterScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var events by remember { mutableStateOf(prefs.getEvents()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<DayCounterEvent?>(null) }

    fun refresh() { events = prefs.getEvents() }

    fun updateWidgets() {
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
                        onEdit = { editingEvent = event; showDialog = true },
                        onDelete = {
                            prefs.deleteEvent(event.id)
                            refresh()
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { editingEvent = null; showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "إضافة مناسبة")
        }
    }

    if (showDialog) {
        AddEventDialog(
            event = editingEvent,
            onDismiss = { showDialog = false; editingEvent = null },
            onSave = { event ->
                prefs.saveEvent(event)
                refresh()
                showDialog = false
                editingEvent = null
                updateWidgets()
            }
        )
    }
}

@Composable
fun EventCard(event: DayCounterEvent, onEdit: () -> Unit, onDelete: () -> Unit) {
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

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "تعديل", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddEventDialog(event: DayCounterEvent? = null, onDismiss: () -> Unit, onSave: (DayCounterEvent) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(event?.name ?: "") }
    var selectedDateMillis by remember { mutableStateOf(event?.date ?: System.currentTimeMillis()) }
    var isCountdown by remember { mutableStateOf(event?.isCountdown ?: true) }
    var dateText by remember { mutableStateOf(formatDateFull(event?.date ?: System.currentTimeMillis())) }
    var widgetTextColor by remember { mutableStateOf(event?.widgetTextColor ?: 0xFFFFFFFF.toInt()) }
    var widgetBgColor by remember { mutableStateOf(event?.widgetBgColor ?: 0xCC000000.toInt()) }
    var widgetFontSize by remember { mutableStateOf(event?.widgetFontSize ?: 32) }
    var widgetBgTransparency by remember { mutableStateOf(event?.widgetBgTransparency ?: 80) }
    val isEdit = event != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "تعديل المناسبة" else "إضافة مناسبة") },
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

                Text("نوع العد", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = isCountdown,
                        onClick = { isCountdown = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("تنازلي (متبقي)") }
                    SegmentedButton(
                        selected = !isCountdown,
                        onClick = { isCountdown = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("تصاعدي (مضى)") }
                }

                Text("تخصيص الـ widget", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("لون الخط", style = MaterialTheme.typography.bodySmall)
                ColorPickerGrid(selectedColor = widgetTextColor, onColorSelected = { widgetTextColor = it })
                Spacer(Modifier.height(4.dp))
                Text("لون الخلفية", style = MaterialTheme.typography.bodySmall)
                ColorPickerGrid(selectedColor = widgetBgColor, onColorSelected = { widgetBgColor = it })

                Spacer(Modifier.height(8.dp))
                Text("حجم خط الأيام", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(24 to "صغير", 32 to "متوسط", 40 to "كبير", 48 to "كبير جداً").forEach { (size, label) ->
                        FilterChip(
                            selected = widgetFontSize == size,
                            onClick = { widgetFontSize = size },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("شفافية الخلفية", style = MaterialTheme.typography.bodySmall)
                Text("${widgetBgTransparency}%", style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = widgetBgTransparency.toFloat(),
                    onValueChange = { widgetBgTransparency = it.toInt() },
                    valueRange = 20f..100f,
                    steps = 7
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    onSave(
                        DayCounterEvent(
                            id = event?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name,
                            date = selectedDateMillis,
                            isCountdown = isCountdown,
                            widgetId = event?.widgetId ?: -1,
                            widgetTextColor = widgetTextColor,
                            widgetBgColor = widgetBgColor,
                            widgetFontSize = widgetFontSize,
                            widgetBgTransparency = widgetBgTransparency
                        )
                    )
                }
            ) { Text(if (isEdit) "حفظ التعديل" else "حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
