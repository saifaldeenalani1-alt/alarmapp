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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alarmapp.data.PreferencesManager
import com.alarmapp.model.DayCounterEvent
import com.alarmapp.receiver.DayCounterReceiver
import com.alarmapp.util.formatDateFull
import java.util.Calendar

private fun scheduleEventNotification(context: Context, event: DayCounterEvent) {
    if (!event.isCountdown || !event.notifyOnComplete) return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val requestCode = event.id.hashCode()
    val intent = Intent(context, DayCounterReceiver::class.java).apply {
        putExtra("event_id", event.id)
        putExtra("event_name", event.name)
    }
    val pi = PendingIntent.getBroadcast(
        context, requestCode, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val cal = Calendar.getInstance().apply { timeInMillis = event.date }
    cal.set(Calendar.HOUR_OF_DAY, 12)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
}

private fun cancelEventNotification(context: Context, eventId: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, DayCounterReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context, eventId.hashCode(), intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    pi?.let {
        alarmManager.cancel(it)
        it.cancel()
    }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onEdit = { editingEvent = event; showDialog = true },
                        onDelete = {
                            cancelEventNotification(context, event.id)
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
                cancelEventNotification(context, event.id)
                prefs.saveEvent(event)
                scheduleEventNotification(context, event)
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
                    text = if (event.notifyOnComplete) "متبقي (تنبيه عند الانتهاء)" else if (event.isCountdown) "متبقي" else "مضى",
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
    var notifyOnComplete by remember { mutableStateOf(event?.notifyOnComplete ?: false) }
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("تنبيه عند انتهاء العداد", modifier = Modifier.weight(1f))
                    Switch(
                        checked = notifyOnComplete,
                        onCheckedChange = { notifyOnComplete = it },
                        enabled = isCountdown
                    )
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
                            notifyOnComplete = notifyOnComplete
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
