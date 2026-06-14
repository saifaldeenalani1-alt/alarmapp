package com.alarmapp.ui.screens

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alarmapp.data.PreferencesManager
import com.alarmapp.model.DayCounterEvent
import com.alarmapp.receiver.DayCounterReceiver
import com.alarmapp.util.formatDateFull
import java.util.Calendar

private const val COMPLETION_REQUEST_BASE = 1000

private fun scheduleEventAlarms(context: Context, event: DayCounterEvent) {
    cancelEventAlarms(context, event.id)

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val eventCal = Calendar.getInstance().apply { timeInMillis = event.date }

    // Completion alarm / notification at noon on event date
    if (event.isCountdown && (event.notifyOnComplete || event.alarmEnabled)) {
        val completionIntent = Intent(context, DayCounterReceiver::class.java).apply {
            putExtra("event_id", event.id)
            putExtra("event_name", event.name)
            putExtra("alarm_enabled", event.alarmEnabled)
            putExtra("type", "completion")
        }
        val pi = PendingIntent.getBroadcast(
            context, event.id.hashCode(), completionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cal = eventCal.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        if (event.alarmEnabled) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, pi), pi)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    // First daily reminder - subsequent reminders scheduled by DayCounterReceiver
    if (event.reminderIntervalDays > 0 && event.isCountdown) {
        val now = Calendar.getInstance()
        val diffMs = eventCal.timeInMillis - now.timeInMillis
        val daysUntil = (diffMs / (1000 * 60 * 60 * 24)).toInt()
        if (daysUntil > 0) {
            val firstReminderDay = minOf(event.reminderIntervalDays, daysUntil)
            val reminderCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, firstReminderDay)
                set(Calendar.HOUR_OF_DAY, 10)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val reminderIntent = Intent(context, DayCounterReceiver::class.java).apply {
                putExtra("event_id", event.id)
                putExtra("event_name", event.name)
                putExtra("days_remaining", (daysUntil - firstReminderDay).toLong())
                putExtra("total_days", daysUntil.toLong())
                putExtra("is_countdown", event.isCountdown)
                putExtra("reminder_interval_days", event.reminderIntervalDays)
                putExtra("event_date", event.date)
                putExtra("type", "reminder")
            }
            val pi = PendingIntent.getBroadcast(
                context, event.id.hashCode() + 2000, reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderCal.timeInMillis, pi)
        }
    }
}

private fun cancelEventAlarms(context: Context, eventId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, DayCounterReceiver::class.java)

    // Cancel completion
    val pi = PendingIntent.getBroadcast(
        context, eventId.hashCode(), intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    pi?.let { alarmManager.cancel(it); it.cancel() }

    // Cancel reminder
    val pi2 = PendingIntent.getBroadcast(
        context, eventId.hashCode() + 2000, intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    pi2?.let { alarmManager.cancel(it); it.cancel() }
}

@Composable
fun DayCounterScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var events by remember { mutableStateOf(prefs.getEvents()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<DayCounterEvent?>(null) }

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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onEdit = { editingEvent = event; showDialog = true },
                        onDelete = {
                            cancelEventAlarms(context, event.id)
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
                cancelEventAlarms(context, event.id)
                prefs.saveEvent(event)
                scheduleEventAlarms(context, event)
                refresh()
                showDialog = false
                editingEvent = null
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

    val displayDays = if (diffDays < 0) 0 else diffDays
    val label = if (event.isCountdown) "يوم متبقي" else "يوم مضى"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = displayDays.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = event.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDateFull(event.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
            if (event.alarmEnabled || event.reminderIntervalDays > 0 || event.notifyOnComplete) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (event.alarmEnabled) {
                        AssistChip(
                            onClick = {},
                            label = { Text("منبه", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    if (event.notifyOnComplete) {
                        AssistChip(
                            onClick = {},
                            label = { Text("إشعار", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    if (event.reminderIntervalDays > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("تذكير كل ${event.reminderIntervalDays} يوم", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }

    // Edit/Delete overlay
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
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
    var notifyOnComplete by remember { mutableStateOf(event?.notifyOnComplete ?: false) }
    var alarmEnabled by remember { mutableStateOf(event?.alarmEnabled ?: false) }
    var reminderIntervalDays by remember { mutableStateOf(event?.reminderIntervalDays ?: 0) }
    var dateText by remember { mutableStateOf(formatDateFull(event?.date ?: System.currentTimeMillis())) }
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

                if (isCountdown) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("منبه عند حلول التاريخ", modifier = Modifier.weight(1f))
                        Switch(
                            checked = alarmEnabled,
                            onCheckedChange = { alarmEnabled = it }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("إشعار عند انتهاء العداد", modifier = Modifier.weight(1f))
                        Switch(
                            checked = notifyOnComplete,
                            onCheckedChange = { notifyOnComplete = it }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("تذكير يومي", modifier = Modifier.weight(1f))
                        Switch(
                            checked = reminderIntervalDays > 0,
                            onCheckedChange = { reminderIntervalDays = if (it) 1 else 0 }
                        )
                    }
                    if (reminderIntervalDays > 0) {
                        Text("الفاصل بين التذكيرات (أيام): $reminderIntervalDays",
                            style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            FilledIconButton(onClick = { if (reminderIntervalDays > 1) reminderIntervalDays-- }) {
                                Icon(Icons.Default.Remove, "تقليل")
                            }
                            Text("$reminderIntervalDays", style = MaterialTheme.typography.titleMedium)
                            FilledIconButton(onClick = { reminderIntervalDays++ }) {
                                Icon(Icons.Default.Add, "زيادة")
                            }
                        }
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
                            id = event?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name,
                            date = selectedDateMillis,
                            isCountdown = isCountdown,
                            notifyOnComplete = notifyOnComplete,
                            alarmEnabled = alarmEnabled,
                            reminderIntervalDays = reminderIntervalDays
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
