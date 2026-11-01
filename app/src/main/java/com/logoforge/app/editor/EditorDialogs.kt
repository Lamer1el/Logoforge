package com.logoforge.app.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PresetColors = listOf(
    0xFF1D1B20, 0xFF49454F, 0xFF79747E, 0xFFCAC4D0, 0xFFFFFFFF,
    0xFF6D3FC4, 0xFF7C4DFF, 0xFFB39DDB, 0xFFEADDFF, 0xFF26005E,
    0xFFD0BCFF, 0xFF6750A4, 0xFF3700B3, 0xFF9FA8DA, 0xFFC5CAE9,
    0xFFD32F2F, 0xFFEF5350, 0xFFFF8A80, 0xFFFFCDD2, 0xFFB71C1C,
    0xFF1976D2, 0xFF42A5F5, 0xFF90CAF9, 0xFFBBDEFB, 0xFF0D47A1,
    0xFF388E3C, 0xFF66BB6A, 0xFFA5D6A7, 0xFFC8E6C9, 0xFF1B5E20,
    0xFFFBC02D, 0xFFFFEB3B, 0xFFFFF59D, 0xFFFFF9C4, 0xFFF57F17,
    0xFFF48FB1, 0xFFEC407A, 0xFFF8BBD0, 0xFFAD1457, 0xFF009688,
    0xFF26C6DA, 0xFFB2DFDB, 0xFF004D40, 0xFF8D6E63, 0xFFD7CCC8,
)

@Composable
fun ColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val initHsv = remember {
        val a = floatArrayOf(0f, 0f, 0f)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.argb(
                255,
                (initial.red * 255).toInt(),
                (initial.green * 255).toInt(),
                (initial.blue * 255).toInt(),
            ),
            a,
        )
        a
    }
    var hue by remember { mutableStateOf(initHsv[0]) }
    var sat by remember { mutableStateOf(initHsv[1]) }
    var value by remember { mutableStateOf(initHsv[2]) }

    fun current(): Color {
        val hsv = floatArrayOf(hue, sat, value)
        val argb = android.graphics.Color.HSVToColor(hsv)
        return Color(argb)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выбор цвета") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp)
                        .background(current(), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                LabeledSlider2("Оттенок", hue, 0f, 360f) { hue = it }
                LabeledSlider2("Насыщенность", sat, 0f, 1f) { sat = it }
                LabeledSlider2("Яркость", value, 0f, 1f) { value = it }

                Spacer(Modifier.height(10.dp))
                Text("Палитра", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                // 8 колонок
                PresetColors.chunked(8).forEach { rowColors ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        rowColors.forEach { c ->
                            Box(
                                Modifier.weight(1f).padding(2.dp)
                                    .size(30.dp)
                                    .background(Color(c), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .clickable {
                                        val argb = android.graphics.Color.argb(
                                            255,
                                            android.graphics.Color.red(c.toInt()),
                                            android.graphics.Color.green(c.toInt()),
                                            android.graphics.Color.blue(c.toInt()),
                                        )
                                        val hsv = floatArrayOf(0f, 0f, 0f)
                                        android.graphics.Color.colorToHSV(argb, hsv)
                                        hue = hsv[0]; sat = hsv[1]; value = hsv[2]
                                    },
                            )
                        }
                        // добиваем пустыми ячейками, чтобы ряды не растягивались
                        repeat(8 - rowColors.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(current()) }) { Text("Готово") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun LabeledSlider2(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label: ${"%.0f".format(value)}", style = MaterialTheme.typography.labelSmall)
        Slider(value = value.coerceIn(min, max), onValueChange = onChange, valueRange = min..max)
    }
}
