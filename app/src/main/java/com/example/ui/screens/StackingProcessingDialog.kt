package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.model.StackingProgress
import com.example.core.model.StackingStage
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.LunarSilver
import com.example.ui.theme.NeonCyan

@Composable
fun StackingProcessingDialog(
    progress: StackingProgress,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onOpenInPngStudio: ((String) -> Unit)? = null
) {
    Dialog(onDismissRequest = {
        if (progress.stage == StackingStage.COMPLETED || progress.stage == StackingStage.FAILED) {
            onDismiss()
        }
    }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GraphiteSurface, RoundedCornerShape(20.dp))
                .border(1.5.dp, CosmicBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .testTag("stacking_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Stacking",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("stacking_title", language),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (progress.stage == StackingStage.COMPLETED || progress.stage == StackingStage.FAILED) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = LunarSilver)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Animated Spinner or Status Icon
                val transition = rememberInfiniteTransition(label = "spin")
                val rotation by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing)
                    ),
                    label = "spinRot"
                )

                when (progress.stage) {
                    StackingStage.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    StackingStage.FAILED -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Failed",
                            tint = CrimsonAlert,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .rotate(rotation)
                                .border(3.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(ElectricViolet, CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val stageLabel = when (progress.stage) {
                    StackingStage.CAPTURING -> AppStrings.get("stack_capturing", language)
                    StackingStage.ANALYZING -> "অস্পষ্ট ফ্রেম পরীক্ষা করা হচ্ছে (Analyzing sharpness)"
                    StackingStage.ALIGNING -> AppStrings.get("stack_aligning", language)
                    StackingStage.BLENDING -> AppStrings.get("stack_blending", language)
                    StackingStage.SHARPENING -> AppStrings.get("stack_sharpening", language)
                    StackingStage.COMPLETED -> AppStrings.get("stack_complete", language)
                    StackingStage.FAILED -> progress.errorMessage ?: "ব্যর্থ হয়েছে"
                    else -> AppStrings.get("stacking_in_progress", language)
                }

                Text(
                    text = stageLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                if (progress.stage != StackingStage.COMPLETED && progress.stage != StackingStage.FAILED) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = NeonCyan,
                        trackColor = CosmicBorder
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metrics / Statistics Breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(AppStrings.get("stack_stats_aligned", language), color = LunarSilver, fontSize = 12.sp)
                        Text("${progress.alignedFrames} / ${progress.totalFrames}", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(AppStrings.get("stack_stats_rejected", language), color = LunarSilver, fontSize = 12.sp)
                        Text("${progress.rejectedFrames}", color = CrimsonAlert, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (progress.stage == StackingStage.COMPLETED) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(AppStrings.get("stack_stats_noise", language), color = LunarSilver, fontSize = 12.sp)
                            Text("~${progress.noiseReductionPercent}%", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                if (progress.stage == StackingStage.COMPLETED && progress.resultFilePath != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ঠিক আছে", color = Color.White, fontSize = 12.sp)
                        }
                        if (onOpenInPngStudio != null) {
                            Button(
                                onClick = { onOpenInPngStudio(progress.resultFilePath) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text(AppStrings.get("open_in_png", language), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
