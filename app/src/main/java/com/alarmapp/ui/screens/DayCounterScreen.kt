package com.alarmapp.ui.screens

import android.app.DatePickerDialog
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مناسبة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                            isCountdown = isCountdown
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
