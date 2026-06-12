package com.alarmapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.alarmapp.MainActivity
import com.alarmapp.R
import com.alarmapp.data.PreferencesManager
import com.alarmapp.model.DayCounterEvent
import java.util.Calendar

class DayCounterWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = PreferencesManager(context)
        val events = prefs.getEvents().toMutableList()
        val unassignedEvents = events.filter { it.widgetId == -1 }.toMutableList()

        appWidgetIds.forEach { widgetId ->
            val existing = events.firstOrNull { it.widgetId == widgetId }
            val event: DayCounterEvent?
            if (existing != null) {
                event = existing
            } else if (unassignedEvents.isNotEmpty()) {
                event = unassignedEvents.removeAt(0)
                val idx = events.indexOfFirst { it.id == event.id }
                if (idx >= 0) {
                    events[idx] = event.copy(widgetId = widgetId)
                    prefs.saveEvent(events[idx])
                }
            } else {
                event = events.firstOrNull()
            }
            updateWidget(context, appWidgetManager, widgetId, event)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        event: DayCounterEvent?
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_day_counter)

        if (event != null) {
            val now = Calendar.getInstance()
            val eventCal = Calendar.getInstance().apply { timeInMillis = event.date }
            val diff: Long
            val isCountdown = event.isCountdown
            val countFieldId = R.id.widget_days_count
            val labelFieldId = R.id.widget_days_label

            if (isCountdown) {
                diff = (eventCal.timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)
                views.setTextViewText(labelFieldId, "يوم متبقي")
            } else {
                diff = (now.timeInMillis - eventCal.timeInMillis) / (1000 * 60 * 60 * 24)
                views.setTextViewText(labelFieldId, "يوم مضى")
            }

            views.setTextViewText(R.id.widget_event_name, event.name)
            views.setTextViewText(countFieldId, if (diff < 0) "0" else diff.toString())
            views.setTextColor(R.id.widget_event_name, event.widgetTextColor)
            views.setTextColor(countFieldId, event.widgetTextColor)
            views.setTextColor(labelFieldId, event.widgetTextColor)
            views.setTextViewTextSize(countFieldId, android.util.TypedValue.COMPLEX_UNIT_SP, event.widgetFontSize.toFloat())

            val alpha = event.widgetBgTransparency / 100f
            val bgColor = (event.widgetBgColor and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg)
            views.setColorStateList(R.id.widget_root, "setBackgroundTintList",
                android.content.res.ColorStateList.valueOf(bgColor))
        } else {
            views.setTextViewText(R.id.widget_event_name, context.getString(R.string.day_counter))
            views.setTextViewText(R.id.widget_days_count, "--")
            views.setTextViewText(R.id.widget_days_label, context.getString(R.string.add_event))
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    override fun onEnabled(context: Context) { }
    override fun onDisabled(context: Context) { }
}
