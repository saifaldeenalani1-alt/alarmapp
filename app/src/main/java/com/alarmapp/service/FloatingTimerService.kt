package com.alarmapp.service

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.alarmapp.AlarmApp
import com.alarmapp.R
import com.alarmapp.data.PreferencesManager
import com.alarmapp.model.AppSettings
import com.alarmapp.model.FloatingTimerState
import com.alarmapp.model.TimerMode
import com.alarmapp.model.TimerStatus
import com.alarmapp.util.formatTimeShort

class FloatingTimerService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var timerText: TextView
    private lateinit var statusText: TextView
    private lateinit var prefs: PreferencesManager
    private var params: WindowManager.LayoutParams? = null

    private var totalSeconds: Long = 0
    private var elapsedSeconds: Long = 0
    private var isCountdown: Boolean = true
    private var isRunning: Boolean = false
    private var isPaused: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                if (isCountdown) {
                    elapsedSeconds++
                    if (elapsedSeconds >= totalSeconds) {
                        elapsedSeconds = totalSeconds
                        isRunning = false
                        isPaused = false
                        timerText.text = "00:00:00"
                        statusText.text = getString(R.string.tap_to_start_stop)
                        stopSelf()
                        return
                    }
                } else {
                    elapsedSeconds++
                }
                updateDisplay()
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = PreferencesManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        totalSeconds = intent?.getLongExtra("total_seconds", 0) ?: 0
        elapsedSeconds = intent?.getLongExtra("elapsed_seconds", 0) ?: 0
        isCountdown = intent?.getBooleanExtra("is_countdown", true) ?: true

        showFloatingView()

        val notification = NotificationCompat.Builder(this, AlarmApp.CHANNEL_FLOATING_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.floating_timer_active))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(3, notification)
        return START_STICKY
    }

    private fun showFloatingView() {
        if (::floatingView.isInitialized) return

        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.floating_timer, null)

        timerText = floatingView.findViewById(R.id.floating_timer_text)
        statusText = floatingView.findViewById(R.id.floating_status_text)

        val settings = prefs.getSettings()
        applyCustomizations(settings)
        updateDisplay()

        val density = resources.displayMetrics.density
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false
            private var longPressTimer: java.util.Timer? = null

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params?.x ?: 0
                        initialY = params?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false

                        longPressTimer?.cancel()
                        longPressTimer = java.util.Timer()
                        longPressTimer?.schedule(object : java.util.TimerTask() {
                            override fun run() {
                                handler.post {
                                    stopSelf()
                                }
                            }
                        }, 2000)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isDragging = true
                            longPressTimer?.cancel()

                            val displayMetrics = resources.displayMetrics
                            val newX = (initialX + dx).coerceIn(0, displayMetrics.widthPixels - floatingView.width)
                            val newY = (initialY + dy).coerceIn(0, displayMetrics.heightPixels - floatingView.height)

                            params?.x = newX
                            params?.y = newY
                            windowManager.updateViewLayout(floatingView, params)
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        longPressTimer?.cancel()
                        val dy = event.rawY - initialTouchY
                        if (dy > 300) {
                            stopSelf()
                            return true
                        }
                        if (!isDragging) {
                            toggleTimer()
                        }
                    }
                }
                return true
            }
        })

        try {
            windowManager.addView(floatingView, params)
        } catch (_: Exception) { }
    }

    private fun toggleTimer() {
        if (!isRunning) {
            isRunning = true
            isPaused = false
            statusText.text = getString(R.string.tap_to_start_stop)
            val remaining = if (isCountdown) totalSeconds - elapsedSeconds else elapsedSeconds
            timerText.text = formatTimeShort(remaining)
            handler.post(tickRunnable)
        } else {
            isRunning = false
            isPaused = true
            statusText.text = getString(R.string.tap_to_start_stop)
        }
    }

    private fun updateDisplay() {
        val displaySeconds = if (isCountdown) totalSeconds - elapsedSeconds else elapsedSeconds
        timerText.text = formatTimeShort(displaySeconds)
    }

    private fun applyCustomizations(settings: AppSettings) {
        try {
            timerText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, settings.fontSize.toFloat() + 8f)
            timerText.setTextColor(settings.fontColor)
            floatingView.setBackgroundColor(settings.backgroundColor)
            floatingView.alpha = settings.transparency / 100f
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        if (::floatingView.isInitialized && floatingView.isAttachedToWindow) {
            try { windowManager.removeView(floatingView) } catch (_: Exception) { }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
