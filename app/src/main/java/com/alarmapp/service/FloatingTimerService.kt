package com.alarmapp.service

import android.app.AlertDialog
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.alarmapp.AlarmApp
import com.alarmapp.R
import com.alarmapp.util.formatTimeShort

class FloatingTimerService : Service() {

    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var notificationId = 100
    private var isForeground = false
    private var tickScheduled = false

    private data class TimerInstance(
        val id: String,
        var totalSeconds: Long,
        var elapsedSeconds: Long,
        var startRealtime: Long,
        var isCountdown: Boolean,
        var isRunning: Boolean,
        var isPaused: Boolean,
        val view: View,
        val timerText: TextView,
        var paramX: Int,
        var paramY: Int,
        val fontSize: Int,
        val fontColor: Int,
        val bgColor: Int,
        val bgTransparency: Int
    )

    private val timers = mutableMapOf<String, TimerInstance>()

    companion object {
        private var instance: FloatingTimerService? = null
        const val ACTION_ADD = "com.alarmapp.action.ADD_TIMER"
        const val ACTION_REMOVE = "com.alarmapp.action.REMOVE_TIMER"
        const val ACTION_STOP_ALL = "com.alarmapp.action.STOP_ALL"

        fun getActiveTimerIds(): Set<String> = instance?.timers?.keys?.toSet() ?: emptySet()

        fun getTimerInfo(id: String): Pair<Long, Boolean>? {
            val t = instance?.timers?.get(id) ?: return null
            val extra = if (t.isRunning) (SystemClock.elapsedRealtime() - t.startRealtime) / 1000 else 0
            val current = t.elapsedSeconds + extra
            val display = if (t.isCountdown) t.totalSeconds - current else current
            return Pair(display, t.isRunning)
        }

        fun start(
            context: android.content.Context,
            timerId: String,
            totalSeconds: Long,
            isCountdown: Boolean,
            fontSize: Int = 24,
            fontColor: Int = 0xFFFFFFFF.toInt(),
            bgColor: Int = 0xCC000000.toInt(),
            bgTransparency: Int = 80
        ) {
            val intent = Intent(context, FloatingTimerService::class.java).apply {
                action = ACTION_ADD
                putExtra("timer_id", timerId)
                putExtra("total_seconds", totalSeconds)
                putExtra("elapsed_seconds", 0L)
                putExtra("is_countdown", isCountdown)
                putExtra("font_size", fontSize)
                putExtra("font_color", fontColor)
                putExtra("bg_color", bgColor)
                putExtra("bg_transparency", bgTransparency)
            }
            context.startForegroundService(intent)
        }

        fun remove(context: android.content.Context, timerId: String) {
            val intent = Intent(context, FloatingTimerService::class.java).apply {
                action = ACTION_REMOVE
                putExtra("timer_id", timerId)
            }
            context.startService(intent)
        }

        fun stopAll(context: android.content.Context) {
            context.startService(Intent(context, FloatingTimerService::class.java).apply {
                action = ACTION_STOP_ALL
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD -> handleAddTimer(intent)
            ACTION_REMOVE -> handleRemoveTimer(intent)
            ACTION_STOP_ALL -> handleStopAll()
        }
        return if (timers.isEmpty()) START_NOT_STICKY else START_STICKY
    }

    private fun handleAddTimer(intent: Intent) {
        val timerId = intent.getStringExtra("timer_id") ?: return
        if (timers.containsKey(timerId)) return

        val totalSeconds = intent.getLongExtra("total_seconds", 0)
        val elapsedSeconds = intent.getLongExtra("elapsed_seconds", 0)
        val isCountdown = intent.getBooleanExtra("is_countdown", true)
        val fontSize = intent.getIntExtra("font_size", 24)
        val fontColor = intent.getIntExtra("font_color", 0xFFFFFFFF.toInt())
        val bgColor = intent.getIntExtra("bg_color", 0xCC000000.toInt())
        val bgTransparency = intent.getIntExtra("bg_transparency", 80)

        if (totalSeconds <= 0) return

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.floating_timer, null)
        val timerText = view.findViewById<TextView>(R.id.floating_timer_text)

        timerText.text = formatTimeShort(if (isCountdown) totalSeconds else 0)
        timerText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, fontSize.toFloat())
        timerText.setTextColor(fontColor)

        val density = resources.displayMetrics.density
        val alpha = bgTransparency / 100f
        val bgColorWithAlpha = (bgColor and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColorWithAlpha)
            cornerRadius = 16 * density
        }
        view.background = bg

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100 + (timers.size * 40)
            y = 200 + (timers.size * 40)
        }

        val instance = TimerInstance(
            id = timerId,
            totalSeconds = totalSeconds,
            elapsedSeconds = elapsedSeconds,
            startRealtime = 0,
            isCountdown = isCountdown,
            isRunning = false,
            isPaused = false,
            view = view,
            timerText = timerText,
            paramX = params.x,
            paramY = params.y,
            fontSize = fontSize,
            fontColor = fontColor,
            bgColor = bgColor,
            bgTransparency = bgTransparency
        )

        setupTouchHandler(instance, params)
        timers[timerId] = instance

        try {
            windowManager.addView(view, params)
        } catch (_: Exception) { }

        startForegroundIfNeeded()
    }

    private fun setupTouchHandler(instance: TimerInstance, params: WindowManager.LayoutParams) {
        instance.view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false
            private var longPressTriggered = false
            private val longPressHandler = Handler(Looper.getMainLooper())
            private val longPressRunnable = Runnable {
                longPressTriggered = true
                showDeleteConfirmation(instance)
            }

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        longPressTriggered = false
                        longPressHandler.postDelayed(longPressRunnable, 800L)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isDragging = true
                            longPressHandler.removeCallbacks(longPressRunnable)
                            val displayMetrics = resources.displayMetrics
                            params.x = (initialX + dx).coerceIn(0, displayMetrics.widthPixels - instance.view.width)
                            params.y = (initialY + dy).coerceIn(0, displayMetrics.heightPixels - instance.view.height)
                            windowManager.updateViewLayout(instance.view, params)
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        longPressHandler.removeCallbacks(longPressRunnable)
                        if (!isDragging && !longPressTriggered) {
                            toggleTimer(instance)
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        longPressHandler.removeCallbacks(longPressRunnable)
                    }
                }
                return true
            }
        })
    }

    private fun showDeleteConfirmation(instance: TimerInstance) {
        val dialog = AlertDialog.Builder(this).apply {
            setTitle("حذف المؤقت")
            setMessage("هل تريد حذف هذا المؤقت؟")
            setPositiveButton("حذف") { _, _ ->
                removeTimer(instance.id)
            }
            setNegativeButton("إلغاء", null)
        }.create()

        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun currentElapsed(instance: TimerInstance): Long {
        val extra = if (instance.isRunning) (SystemClock.elapsedRealtime() - instance.startRealtime) / 1000 else 0
        return instance.elapsedSeconds + extra
    }

    private fun toggleTimer(instance: TimerInstance) {
        if (!instance.isRunning) {
            if (instance.isCountdown && currentElapsed(instance) >= instance.totalSeconds) {
                instance.elapsedSeconds = 0
            }
            instance.startRealtime = SystemClock.elapsedRealtime()
            instance.isRunning = true
            instance.isPaused = false
            updateViewAppearance(instance)
            updateDisplay(instance)
            scheduleTick()
        } else {
            instance.elapsedSeconds = currentElapsed(instance)
            instance.startRealtime = 0
            instance.isRunning = false
            instance.isPaused = true
            updateViewAppearance(instance)
        }
    }

    private fun updateViewAppearance(instance: TimerInstance) {
        val density = resources.displayMetrics.density
        val alpha = instance.bgTransparency / 100f
        val bgColorWithAlpha = (instance.bgColor and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)

        if (instance.isRunning) {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(bgColorWithAlpha)
                cornerRadius = 16 * density
            }
            instance.view.background = bg
        } else {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(bgColorWithAlpha)
                cornerRadius = 16 * density
                setStroke(3 * density.toInt(), 0xFFFF6D00.toInt())
            }
            instance.view.background = bg
        }
    }

    private fun scheduleTick() {
        if (tickScheduled) return
        tickScheduled = true
        handler.post(tickRunnable)
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            tickScheduled = false
            var anyRunning = false
            for (timer in timers.values) {
                if (timer.isRunning) {
                    anyRunning = true
                    val current = currentElapsed(timer)
                    if (timer.isCountdown && current >= timer.totalSeconds) {
                        timer.elapsedSeconds = timer.totalSeconds
                        timer.startRealtime = SystemClock.elapsedRealtime()
                        timer.isRunning = false
                        timer.isPaused = false
                        updateViewAppearance(timer)
                    }
                    updateDisplay(timer)
                }
            }
            if (anyRunning) {
                handler.postDelayed(this, 1000L)
                tickScheduled = true
            } else {
                updateNotification()
            }
        }
    }

    private fun updateDisplay(instance: TimerInstance) {
        val current = currentElapsed(instance)
        val display = if (instance.isCountdown) instance.totalSeconds - current else current

        if (instance.isCountdown && current >= instance.totalSeconds && !instance.isRunning) {
            instance.timerText.text = formatTimeShort(0)
        } else {
            instance.timerText.text = formatTimeShort(display)
        }

        if (instance.isCountdown && instance.isRunning && display <= 10) {
            val flashColor = (if ((display % 2) == 0L) 0xFFFF0000.toInt() else instance.fontColor)
            instance.timerText.setTextColor(flashColor)
        } else {
            instance.timerText.setTextColor(instance.fontColor)
        }
    }

    private fun removeTimer(timerId: String) {
        val instance = timers.remove(timerId) ?: return
        handler.removeCallbacksAndMessages(null)
        tickScheduled = false
        if (instance.view.isAttachedToWindow) {
            try { windowManager.removeView(instance.view) } catch (_: Exception) { }
        }
        if (timers.isEmpty()) {
            stopSelf()
        } else {
            if (timers.values.any { it.isRunning }) {
                scheduleTick()
            }
            updateNotification()
        }
    }

    private fun handleRemoveTimer(intent: Intent) {
        val timerId = intent.getStringExtra("timer_id") ?: return
        removeTimer(timerId)
    }

    private fun handleStopAll() {
        for (instance in timers.values) {
            if (instance.view.isAttachedToWindow) {
                try { windowManager.removeView(instance.view) } catch (_: Exception) { }
            }
        }
        timers.clear()
        handler.removeCallbacksAndMessages(null)
        tickScheduled = false
        stopSelf()
    }

    private fun startForegroundIfNeeded() {
        if (!isForeground) {
            isForeground = true
            startForeground(notificationId, buildNotification())
        } else {
            updateNotification()
        }
    }

    private fun buildNotification(): Notification {
        val count = timers.size
        val title = if (count == 0) getString(R.string.floating_timer_active)
            else "$count مؤقتات نشطة"
        val running = timers.values.count { it.isRunning }
        val text = if (running == 0) "متوقف" else "$running مؤقت يعمل"

        return NotificationCompat.Builder(this, AlarmApp.CHANNEL_FLOATING_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm?.notify(notificationId, buildNotification())
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tickScheduled = false
        for (timer in timers.values) {
            if (timer.view.isAttachedToWindow) {
                try { windowManager.removeView(timer.view) } catch (_: Exception) { }
            }
        }
        timers.clear()
        instance = null
        isForeground = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
