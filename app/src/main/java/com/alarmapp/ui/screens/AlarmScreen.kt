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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import android.media.MediaPlayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
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
    val dayNames = arrayOf("", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    val shortNames = arrayOf("", "ح", "ن", "ث", "ر", "خ", "ج", "س")

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
                    text = "${"%02d".format(alarm.startHour)}:${"%02d".format(alarm.startMinute)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (alarm.repeatDays.isNotEmpty()) {
                    Text(
                        text = alarm.repeatDays.sorted().joinToString(" - ") { dayNames[it] },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (alarm.label.isNotEmpty()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAlarmDialog(alarm: Alarm? = null, onDismiss: () -> Unit, onSave: (Alarm) -> Unit) {
    val context = LocalContext.current
    var intervalMinutes by remember { mutableIntStateOf(alarm?.intervalMinutes ?: 60) }
    var startHour by remember { mutableIntStateOf(alarm?.startHour ?: 8) }
    var startMinute by remember { mutableIntStateOf(alarm?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(alarm?.endHour ?: 22) }
    var endMinute by remember { mutableIntStateOf(alarm?.endMinute ?: 0) }
    var repeatDays by remember { mutableStateOf(alarm?.repeatDays ?: emptySet()) }
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var toneUri by remember { mutableStateOf(alarm?.toneUri ?: "") }
    var vibrate by remember { mutableStateOf(alarm?.vibrate ?: true) }
    var muteInSilentMode by remember { mutableStateOf(alarm?.muteInSilentMode ?: false) }
    var currentPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val isRepeating = repeatDays.isNotEmpty()
    val dayNames = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    val dayValues = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("اسم المنبه (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(
                        "⏰ ${"%02d".format(startHour)}:${"%02d".format(startMinute)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("تكرار", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isRepeating,
                        onCheckedChange = { if (it) repeatDays = setOf(Calendar.SUNDAY) else repeatDays = emptySet() }
                    )
                }

                if (isRepeating) {
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مدى العمل حتى ${"%02d".format(endHour)}:${"%02d".format(endMinute)}")
                    }

                    Text("أيام التكرار", style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayValues.forEachIndexed { i, dayValue ->
                            FilterChip(
                                selected = dayValue in repeatDays,
                                onClick = {
                                    repeatDays = if (dayValue in repeatDays) repeatDays - dayValue
                                    else repeatDays + dayValue
                                },
                                label = { Text(dayNames[i], style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

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
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الاهتزاز", modifier = Modifier.weight(1f))
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("كتم الصوت في الوضع الصامت", modifier = Modifier.weight(1f))
                    Switch(checked = muteInSilentMode, onCheckedChange = { muteInSilentMode = it })
                }

                Text("النغمات المدمجة", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = toneUri.contains("tone1"),
                        onClick = {
                            toneUri = "android.resource://${context.packageName}/raw/tone1"
                            currentPlayer?.release()
                            currentPlayer = try {
                                MediaPlayer.create(context, R.raw.tone1)?.apply {
                                    setOnCompletionListener { release(); currentPlayer = null }
                                    start()
                                }
                            } catch (_: Exception) { null }
                        },
                        label = { Text("نغمة 1") }
                    )
                    FilterChip(
                        selected = toneUri.contains("tone2"),
                        onClick = {
                            toneUri = "android.resource://${context.packageName}/raw/tone2"
                            currentPlayer?.release()
                            currentPlayer = try {
                                MediaPlayer.create(context, R.raw.tone2)?.apply {
                                    setOnCompletionListener { release(); currentPlayer = null }
                                    start()
                                }
                            } catch (_: Exception) { null }
                        },
                        label = { Text("نغمة 2") }
                    )
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
                        repeatDays = repeatDays,
                        label = label,
                        toneUri = toneUri,
                        vibrate = vibrate,
                        muteInSilentMode = muteInSilentMode
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
