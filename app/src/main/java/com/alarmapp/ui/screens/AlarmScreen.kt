package com.alarmapp.ui.screens

import com.alarmapp.R
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.media.MediaPlayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alarmapp.data.PreferencesManager
import com.alarmapp.model.Alarm
import com.alarmapp.receiver.AlarmReceiver
import com.alarmapp.util.AlarmScheduler
import com.alarmapp.util.formatTimeShort
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var alarms by remember { mutableStateOf(prefs.getAlarms()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    fun refresh() { alarms = prefs.getAlarms() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (alarms.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("لا توجد منبهات", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("اضغط على + لإضافة منبه جديد", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = {
                            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
                            prefs.saveAlarm(updated)
                            if (updated.isEnabled) {
                                AlarmScheduler.scheduleAlarm(context, updated)
                            } else {
                                AlarmScheduler.cancelAlarm(context, updated)
                            }
                            refresh()
                        },
                        onEdit = { editingAlarm = alarm; showAddDialog = true },
                        onDelete = {
                            AlarmScheduler.cancelAlarm(context, alarm)
                            prefs.deleteAlarm(alarm.id)
                            refresh()
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { editingAlarm = null; showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "إضافة منبه")
        }
    }

    if (showAddDialog) {
        AddAlarmDialog(
            alarm = editingAlarm,
            onDismiss = { showAddDialog = false; editingAlarm = null },
            onSave = { alarm ->
                prefs.saveAlarm(alarm)
                if (alarm.isEnabled) {
                    AlarmScheduler.scheduleAlarm(context, alarm)
                } else {
                    AlarmScheduler.cancelAlarm(context, alarm)
                }
                refresh()
                showAddDialog = false
                editingAlarm = null
            }
        )
    }
}

@Composable
fun AlarmCard(alarm: Alarm, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "كل ${alarm.intervalMinutes} دقيقة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "من ${"%02d".format(alarm.startHour)}:${"%02d".format(alarm.startMinute)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (alarm.isScheduled) {
                    Text(
                        text = "إلى ${"%02d".format(alarm.endHour)}:${"%02d".format(alarm.endMinute)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (alarm.label.isNotEmpty()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "تعديل", tint = MaterialTheme.colorScheme.primary)
            }

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() }
            )

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmDialog(alarm: Alarm? = null, onDismiss: () -> Unit, onSave: (Alarm) -> Unit) {
    val context = LocalContext.current
    var intervalMinutes by remember { mutableIntStateOf(alarm?.intervalMinutes ?: 60) }
    var startHour by remember { mutableIntStateOf(alarm?.startHour ?: 8) }
    var startMinute by remember { mutableIntStateOf(alarm?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(alarm?.endHour ?: 22) }
    var endMinute by remember { mutableIntStateOf(alarm?.endMinute ?: 0) }
    var isScheduled by remember { mutableStateOf(alarm?.isScheduled ?: false) }
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var toneUri by remember { mutableStateOf(alarm?.toneUri ?: "") }
    var vibrate by remember { mutableStateOf(alarm?.vibrate ?: true) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val intervalOptions = listOf(5, 10, 15, 30, 60, 120, 180, 360, 720, 1440)
    var expanded by remember { mutableStateOf(false) }
    val tonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            if (uri != null) toneUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (alarm != null) "تعديل المنبه" else "إضافة منبه جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("اسم المنبه (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("الفاصل الزمني")

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = "${intervalMinutes} دقيقة",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        intervalOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option} دقيقة") },
                                onClick = {
                                    intervalMinutes = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = { showStartTimePicker = true }) {
                        Text("بداية: ${"%02d".format(startHour)}:${"%02d".format(startMinute)}")
                    }
                    OutlinedButton(onClick = { showEndTimePicker = true }) {
                        Text("نهاية: ${"%02d".format(endHour)}:${"%02d".format(endMinute)}")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("جدولة بين وقتين", modifier = Modifier.weight(1f))
                    Switch(checked = isScheduled, onCheckedChange = { isScheduled = it })
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الاهتزاز", modifier = Modifier.weight(1f))
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }

                Text("النغمات المدمجة", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = toneUri.contains("tone1"),
                        onClick = { toneUri = "android.resource://${context.packageName}/raw/tone1" },
                        label = { Text("نغمة 1") }
                    )
                    IconButton(onClick = {
                        try {
                            MediaPlayer.create(context, R.raw.tone1)?.apply {
                                setOnCompletionListener { release() }
                                start()
                            }
                        } catch (_: Exception) { }
                    }) {
                        Icon(Icons.Default.PlayArrow, "استماع", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    FilterChip(
                        selected = toneUri.contains("tone2"),
                        onClick = { toneUri = "android.resource://${context.packageName}/raw/tone2" },
                        label = { Text("نغمة 2") }
                    )
                    IconButton(onClick = {
                        try {
                            MediaPlayer.create(context, R.raw.tone2)?.apply {
                                setOnCompletionListener { release() }
                                start()
                            }
                        } catch (_: Exception) { }
                    }) {
                        Icon(Icons.Default.PlayArrow, "استماع", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }

                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر نغمة المنبه")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                if (toneUri.isNotEmpty()) Uri.parse(toneUri) else null)
                        }
                        tonePickerLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (toneUri.isNotEmpty()) "تغيير النغمة" else "اختيار نغمة من النظام")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    Alarm(
                        id = alarm?.id ?: java.util.UUID.randomUUID().toString(),
                        isEnabled = alarm?.isEnabled ?: true,
                        intervalMinutes = intervalMinutes,
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        isScheduled = isScheduled,
                        label = label,
                        toneUri = toneUri,
                        vibrate = vibrate
                    )
                )
            }) { Text(if (alarm != null) "حفظ التعديل" else "حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )

    if (showStartTimePicker) {
        TimePickerDialog(
            context,
            { _, h, m -> startHour = h; startMinute = m; showStartTimePicker = false },
            startHour, startMinute, true
        ).show()
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            context,
            { _, h, m -> endHour = h; endMinute = m; showEndTimePicker = false },
            endHour, endMinute, true
        ).show()
    }
}
