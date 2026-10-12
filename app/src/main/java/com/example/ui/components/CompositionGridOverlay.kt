package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun CompositionGridOverlay(
    showLevel: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val gridColor = Color.White.copy(alpha = 0.22f)
        val strokeWidth = 1.dp.toPx()

        // 3x3 Rule of thirds
        drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth)
        drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth)

        drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth)
        drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth)

        // Artificial Level Horizon line in the center
        if (showLevel) {
            val levelColor = Color(0xFF06B6D4).copy(alpha = 0.65f)
            val cx = w / 2f
            val cy = h / 2f
            val armLength = 28.dp.toPx()

            // Left tick
            drawLine(levelColor, Offset(cx - armLength - 16.dp.toPx(), cy), Offset(cx - 16.dp.toPx(), cy), 2.dp.toPx())
            // Right tick
            drawLine(levelColor, Offset(cx + 16.dp.toPx(), cy), Offset(cx + armLength + 16.dp.toPx(), cy), 2.dp.toPx())
            // Center ring
            drawCircle(levelColor, 4.dp.toPx(), Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
        }
    }
}
