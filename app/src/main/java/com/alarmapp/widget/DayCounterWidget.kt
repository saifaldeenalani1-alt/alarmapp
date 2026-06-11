package com.alarmapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.alarmapp.MainActivity
import com.alarmapp.R
import com.alarmapp.data.PreferencesManager
import java.util.Calendar

class DayCounterWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val prefs = PreferencesManager(context)
        val events = prefs.getEvents()
        val views = RemoteViews(context.packageName, R.layout.widget_day_counter)

        val event = events.firstOrNull { it.widgetId == widgetId } ?: events.firstOrNull()

        if (event != null) {
            val now = Calendar.getInstance()
            val eventCal = Calendar.getInstance().apply { timeInMillis = event.date }
            val diff: Long

            if (event.isCountdown) {
                diff = (eventCal.timeInMillis - now.timeInMillis) / (1000 * 60 * 60 * 24)
                views.setTextViewText(R.id.widget_days_label, "يوم متبقي")
            } else {
                diff = (now.timeInMillis - eventCal.timeInMillis) / (1000 * 60 * 60 * 24)
                views.setTextViewText(R.id.widget_days_label, "يوم مضى")
            }

            views.setTextViewText(R.id.widget_event_name, event.name)
            views.setTextViewText(R.id.widget_days_count, diff.toString())
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
        views.setOnClickPendingIntent(R.id.widget_event_name, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    override fun onEnabled(context: Context) { }
    override fun onDisabled(context: Context) { }
}
