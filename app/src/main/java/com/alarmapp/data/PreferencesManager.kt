package com.alarmapp.data

import android.content.Context
import android.content.SharedPreferences
import com.alarmapp.model.Alarm
import com.alarmapp.model.AppSettings
import com.alarmapp.model.DayCounterEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("alarm_app_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private fun <T> getObject(key: String, type: java.lang.reflect.Type, default: T): T {
        val json = prefs.getString(key, null) ?: return default
        return try { gson.fromJson(json, type) } catch (_: Exception) { default }
    }

    private fun <T> saveObject(key: String, obj: T) {
        prefs.edit().putString(key, gson.toJson(obj)).apply()
    }

    fun getAlarms(): MutableList<Alarm> {
        val type = object : TypeToken<MutableList<Alarm>>() {}.type
        return getObject("alarms", type, mutableListOf())
    }

    fun saveAlarm(alarm: Alarm) {
        val alarms = getAlarms()
        val idx = alarms.indexOfFirst { it.id == alarm.id }
        if (idx >= 0) alarms[idx] = alarm else alarms.add(alarm)
        saveObject("alarms", alarms)
    }

    fun deleteAlarm(id: String) {
        val alarms = getAlarms()
        alarms.removeAll { it.id == id }
        saveObject("alarms", alarms)
    }

    fun getEvents(): MutableList<DayCounterEvent> {
        val type = object : TypeToken<MutableList<DayCounterEvent>>() {}.type
        return getObject("events", type, mutableListOf())
    }

    fun saveEvent(event: DayCounterEvent) {
        val events = getEvents()
        val idx = events.indexOfFirst { it.id == event.id }
        if (idx >= 0) events[idx] = event else events.add(event)
        saveObject("events", events)
    }

    fun deleteEvent(id: String) {
        val events = getEvents()
        events.removeAll { it.id == id }
        saveObject("events", events)
    }

    fun getSettings(): AppSettings {
        val type = object : TypeToken<AppSettings>() {}.type
        return getObject("settings", type, AppSettings())
    }

    fun saveSettings(settings: AppSettings) {
        saveObject("settings", settings)
    }

    fun getFloatingTimerSeconds(): Long = prefs.getLong("floating_timer_seconds", 0)

    fun saveFloatingTimerSeconds(seconds: Long) {
        prefs.edit().putLong("floating_timer_seconds", seconds).apply()
    }
}
