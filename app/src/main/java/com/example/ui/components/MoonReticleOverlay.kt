package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.model.DetectionStatus
import com.example.core.model.MoonDetectionResult
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBackground

@Composable
fun MoonReticleOverlay(
    detectionResult: MoonDetectionResult,
    language: AppLanguage,
    onManualTapToAim: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val reticleColor = when (detectionResult.status) {
        DetectionStatus.DETECTED -> EmeraldSuccess
        DetectionStatus.SEARCHING -> AmberHighlight
        DetectionStatus.LOW_CONFIDENCE -> NeonCyan
        DetectionStatus.PARTIALLY_OBSCURED -> Color(0xFFA78BFA)
        DetectionStatus.NOT_DETECTED -> Color(0xFF64748B)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normX = (offset.x / size.width).coerceIn(0.1f, 0.9f)
                    val normY = (offset.y / size.height).coerceIn(0.1f, 0.9f)
                    onManualTapToAim(normX, normY)
                }
            }
            .testTag("moon_reticle_overlay")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * detectionResult.normalizedCenterX
            val cy = size.height * detectionResult.normalizedCenterY
            val baseRadius = (size.width * detectionResult.normalizedRadius).coerceIn(40.dp.toPx(), 200.dp.toPx())
            val radius = if (detectionResult.status == DetectionStatus.SEARCHING) baseRadius * pulseScale else baseRadius

            // Draw outer tracking ring
            drawCircle(
                color = reticleColor.copy(alpha = 0.85f),
                center = Offset(cx, cy),
                radius = radius,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = if (detectionResult.status == DetectionStatus.SEARCHING) PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f) else null
                )
            )

            // Draw 4 corner reticle brackets
            val bracketLen = 18.dp.toPx()
            val offsetDist = radius + 8.dp.toPx()

            // Top-left
            drawLine(reticleColor, Offset(cx - offsetDist, cy - offsetDist + bracketLen), Offset(cx - offsetDist, cy - offsetDist), 3.dp.toPx())
            drawLine(reticleColor, Offset(cx - offsetDist, cy - offsetDist), Offset(cx - offsetDist + bracketLen, cy - offsetDist), 3.dp.toPx())

            // Top-right
            drawLine(reticleColor, Offset(cx + offsetDist, cy - offsetDist + bracketLen), Offset(cx + offsetDist, cy - offsetDist), 3.dp.toPx())
            drawLine(reticleColor, Offset(cx + offsetDist, cy - offsetDist), Offset(cx + offsetDist - bracketLen, cy - offsetDist), 3.dp.toPx())

            // Bottom-left
            drawLine(reticleColor, Offset(cx - offsetDist, cy + offsetDist - bracketLen), Offset(cx - offsetDist, cy + offsetDist), 3.dp.toPx())
            drawLine(reticleColor, Offset(cx - offsetDist, cy + offsetDist), Offset(cx - offsetDist + bracketLen, cy + offsetDist), 3.dp.toPx())

            // Bottom-right
            drawLine(reticleColor, Offset(cx + offsetDist, cy + offsetDist - bracketLen), Offset(cx + offsetDist, cy + offsetDist), 3.dp.toPx())
            drawLine(reticleColor, Offset(cx + offsetDist, cy + offsetDist), Offset(cx + offsetDist - bracketLen, cy + offsetDist), 3.dp.toPx())

            // Crosshair center dot
            drawCircle(
                color = reticleColor.copy(alpha = 0.9f),
                center = Offset(cx, cy),
                radius = 3.dp.toPx()
            )
        }

        // Status badge at top of reticle
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val statusText = when (detectionResult.status) {
                DetectionStatus.DETECTED -> AppStrings.get("moon_detected", language)
                DetectionStatus.SEARCHING -> AppStrings.get("moon_searching", language)
                DetectionStatus.LOW_CONFIDENCE -> AppStrings.get("moon_low_confidence", language)
                DetectionStatus.PARTIALLY_OBSCURED -> AppStrings.get("moon_partially_obscured", language)
                DetectionStatus.NOT_DETECTED -> AppStrings.get("moon_not_detected", language)
            }

            Box(
                modifier = Modifier
                    .background(ObsidianBackground.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                    .border(1.dp, reticleColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(reticleColor, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Highlight clipping alert if lunar luminance blows out
            if (detectionResult.isClippedHighlight) {
                Spacer(modifier = Modifier.padding(top = 6.dp))
                Box(
                    modifier = Modifier
                        .background(CrimsonAlert.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AppStrings.get("highlight_clipping_alert", language),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
