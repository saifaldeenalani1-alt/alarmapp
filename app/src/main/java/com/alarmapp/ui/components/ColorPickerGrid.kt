package com.alarmapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ColorOption(val name: String, val color: Int)

val fullColorPalette = listOf(
    ColorOption("أبيض", 0xFFFFFFFF.toInt()),
    ColorOption("أسود", 0xFF000000.toInt()),
    ColorOption("رمادي فاتح", 0xFFBDBDBD.toInt()),
    ColorOption("رمادي", 0xFF757575.toInt()),
    ColorOption("رمادي داكن", 0xFF444444.toInt()),
    ColorOption("أحمر", 0xFFD32F2F.toInt()),
    ColorOption("أحمر داكن", 0xFFB71C1C.toInt()),
    ColorOption("وردي", 0xFFE91E63.toInt()),
    ColorOption("وردي فاتح", 0xFFF48FB1.toInt()),
    ColorOption("أرجواني", 0xFF9C27B0.toInt()),
    ColorOption("بنفسجي", 0xFF673AB7.toInt()),
    ColorOption("بنفسجي داكن", 0xFF512DA8.toInt()),
    ColorOption("أزرق", 0xFF1976D2.toInt()),
    ColorOption("أزرق فاتح", 0xFF42A5F5.toInt()),
    ColorOption("أزرق سماوي", 0xFF00BCD4.toInt()),
    ColorOption("كحلي", 0xFF0D47A1.toInt()),
    ColorOption("أخضر", 0xFF388E3C.toInt()),
    ColorOption("أخضر فاتح", 0xFF66BB6A.toInt()),
    ColorOption("أخضر زاهي", 0xFF00C853.toInt()),
    ColorOption("زيتي", 0xFF33691E.toInt()),
    ColorOption("أصفر", 0xFFFDD835.toInt()),
    ColorOption("أصفر داكن", 0xFFF9A825.toInt()),
    ColorOption("برتقالي", 0xFFFF6D00.toInt()),
    ColorOption("برتقالي داكن", 0xFFE65100.toInt()),
    ColorOption("ذهبي", 0xFFFFAB00.toInt()),
    ColorOption("بني", 0xFF5D4037.toInt()),
    ColorOption("نحاسي", 0xFF8D6E63.toInt()),
    ColorOption("نعناعي", 0xFF009688.toInt()),
    ColorOption("ليموني", 0xFF827717.toInt()),
    ColorOption("نيون وردي", 0xFFFF4081.toInt()),
    ColorOption("نيون أزرق", 0xFF448AFF.toInt()),
    ColorOption("نيون أخضر", 0xFF76FF03.toInt()),
)

@Composable
fun ColorPickerGrid(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(fullColorPalette, key = { it.color }) { option ->
                val isSelected = selectedColor == option.color
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .then(
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                        .clickable { onColorSelected(option.color) },
                    color = Color(option.color),
                    shape = CircleShape
                ) {}
            }
        }
    }
}
