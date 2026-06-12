package com.alarmapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ClockScreen() {
    var now by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            delay(1000)
        }
    }

    val dayOfWeekAr = arrayOf("", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    val monthAr = arrayOf("", "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

    val h = now.get(Calendar.HOUR)
    val m = now.get(Calendar.MINUTE)
    val s = now.get(Calendar.SECOND)
    val h24 = now.get(Calendar.HOUR_OF_DAY)
    val amPm = if (now.get(Calendar.AM_PM) == Calendar.AM) "ص" else "م"
    val displayHour = if (h == 0) 12 else h

    val dateText = "${dayOfWeekAr[now.get(Calendar.DAY_OF_WEEK)]}، ${now.get(Calendar.DAY_OF_MONTH)} ${monthAr[now.get(Calendar.MONTH)]} ${now.get(Calendar.YEAR)}"

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val clockSize = (screenWidthDp * 0.85f).coerceAtMost(screenHeightDp * 0.55f).dp

    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(clockSize)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val radius = size.minDimension / 2
                val outerRing = radius * 0.96f
                val handRadius = radius * 0.75f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            surface,
                            surfaceVariant.copy(alpha = 0.3f)
                        ),
                        center = Offset(cx, cy),
                        radius = radius
                    ),
                    radius = radius
                )

                drawCircle(
                    color = primary.copy(alpha = 0.15f),
                    radius = radius,
                    style = Stroke(width = 3.dp.toPx())
                )

                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.3f),
                            primary.copy(alpha = 0.6f),
                            primary.copy(alpha = 0.3f),
                            primary.copy(alpha = 0.6f),
                            primary.copy(alpha = 0.3f)
                        )
                    ),
                    radius = outerRing,
                    style = Stroke(width = 2.dp.toPx())
                )

                val minuteTickLen = radius * 0.06f
                val hourTickLen = radius * 0.12f
                val hourTickWidth = 2.5.dp.toPx()
                val minuteTickWidth = 1.2.dp.toPx()

                val arabicNumerals = listOf("١٢", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩", "١٠", "١١")
                val paint = android.graphics.Paint().apply {
                    color = onSurface.hashCode()
                    textSize = radius * 0.13f
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }

                for (i in 0 until 60) {
                    val angle = Math.toRadians((i * 6).toDouble())
                    val isHour = i % 5 == 0
                    val innerR = if (isHour) radius * 0.82f else radius * 0.86f
                    val outerR = if (isHour) innerR + hourTickLen else innerR + minuteTickLen

                    drawLine(
                        color = if (isHour) onSurface else onSurfaceVariant.copy(alpha = 0.5f),
                        start = Offset(
                            cx + innerR * cos(angle).toFloat(),
                            cy + innerR * sin(angle).toFloat()
                        ),
                        end = Offset(
                            cx + outerR * cos(angle).toFloat(),
                            cy + outerR * sin(angle).toFloat()
                        ),
                        strokeWidth = if (isHour) hourTickWidth else minuteTickWidth
                    )
                }

                for (i in 0 until 12) {
                    val angle = Math.toRadians((i * 30).toDouble())
                    val numR = radius * 0.70f
                    val px = cx + numR * cos(angle).toFloat()
                    val py = cy + numR * sin(angle).toFloat()
                    drawContext.canvas.nativeCanvas.drawText(
                        arabicNumerals[i],
                        px,
                        py + paint.textSize * 0.35f,
                        paint
                    )
                }

                val hoursAngle = Math.toRadians(((h24 % 12) * 30 + m * 0.5).toDouble())
                val minutesAngle = Math.toRadians((m * 6 + s * 0.1).toDouble())
                val secondsAngle = Math.toRadians((s * 6).toDouble())
                val hLen = handRadius * 0.5f
                val mLen = handRadius * 0.7f
                val sLen = handRadius * 0.85f

                drawLine(
                    color = onSurface,
                    start = Offset(cx, cy),
                    end = Offset(
                        cx + hLen * sin(hoursAngle).toFloat(),
                        cy - hLen * cos(hoursAngle).toFloat()
                    ),
                    strokeWidth = 5.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                drawLine(
                    color = onSurface,
                    start = Offset(cx, cy),
                    end = Offset(
                        cx + mLen * sin(minutesAngle).toFloat(),
                        cy - mLen * cos(minutesAngle).toFloat()
                    ),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                drawLine(
                    color = Color(0xFFE53935),
                    start = Offset(cx, cy),
                    end = Offset(
                        cx + sLen * sin(secondsAngle).toFloat(),
                        cy - sLen * cos(secondsAngle).toFloat()
                    ),
                    strokeWidth = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                val centerR = radius * 0.16f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(surfaceVariant, primary.copy(alpha = 0.15f)),
                        center = Offset(cx, cy),
                        radius = centerR
                    ),
                    radius = centerR
                )
                drawCircle(
                    color = primary.copy(alpha = 0.2f),
                    radius = centerR,
                    style = Stroke(width = 1.dp.toPx())
                )

                val centerPaint = android.graphics.Paint().apply {
                    color = onSurface.hashCode()
                    textSize = radius * 0.13f
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                val amPmPaint = android.graphics.Paint().apply {
                    color = onSurfaceVariant.hashCode()
                    textSize = radius * 0.06f
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT
                    isAntiAlias = true
                }
                val timeStr = "%02d:%02d".format(displayHour, m)
                drawContext.canvas.nativeCanvas.drawText(
                    timeStr,
                    cx,
                    cy + centerPaint.textSize * 0.35f,
                    centerPaint
                )
                drawContext.canvas.nativeCanvas.drawText(
                    amPm,
                    cx,
                    cy + centerR * 0.8f,
                    amPmPaint
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dateText,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
