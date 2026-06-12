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

    val h = now.get(Calendar.HOUR_OF_DAY)
    val m = now.get(Calendar.MINUTE)
    val s = now.get(Calendar.SECOND)
    val dotColor = if (s % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    val dateText = "${dayOfWeekAr[now.get(Calendar.DAY_OF_WEEK)]}، ${now.get(Calendar.DAY_OF_MONTH)} ${monthAr[now.get(Calendar.MONTH)]} ${now.get(Calendar.YEAR)}"

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("%02d".format(h), fontSize = 80.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(":", fontSize = 80.sp, fontWeight = FontWeight.Bold, color = dotColor, modifier = Modifier.padding(horizontal = 4.dp))
                Text("%02d".format(m), fontSize = 80.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(":", fontSize = 80.sp, fontWeight = FontWeight.Bold, color = dotColor, modifier = Modifier.padding(horizontal = 4.dp))
                Text("%02d".format(s), fontSize = 80.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
