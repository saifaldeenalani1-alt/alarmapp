package com.alarmapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar

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

    val h12 = now.get(Calendar.HOUR)
    val amPm = if (now.get(Calendar.AM_PM) == Calendar.AM) "ص" else "م"
    val m = now.get(Calendar.MINUTE)
    val s = now.get(Calendar.SECOND)
    val displayHour = if (h12 == 0) 12 else h12
    val dotColor = if (s % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    val dateText = "${dayOfWeekAr[now.get(Calendar.DAY_OF_WEEK)]}، ${now.get(Calendar.DAY_OF_MONTH)} ${monthAr[now.get(Calendar.MONTH)]} ${now.get(Calendar.YEAR)}"
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val timeFontSize = (screenWidthDp / 7).coerceAtMost(72).sp
    val colonFontSize = timeFontSize

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("%02d".format(displayHour), fontSize = timeFontSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(":", fontSize = colonFontSize, fontWeight = FontWeight.Bold, color = dotColor, modifier = Modifier.padding(horizontal = 2.dp))
                Text("%02d".format(m), fontSize = timeFontSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(":", fontSize = colonFontSize, fontWeight = FontWeight.Bold, color = dotColor, modifier = Modifier.padding(horizontal = 2.dp))
                Text("%02d".format(s), fontSize = timeFontSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(amPm, fontSize = (timeFontSize * 0.4f).sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = dateText,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}
