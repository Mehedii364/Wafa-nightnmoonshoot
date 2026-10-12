package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.model.CaptureMode
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.DeepSpaceBlack
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.LunarSilver
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBackground

@Composable
fun CameraControlsHud(
    captureMode: CaptureMode,
    currentZoom: Float,
    maxOpticalZoom: Float,
    exposureCompensation: Float,
    stackingFrames: Int,
    timerSeconds: Int,
    isTorchOn: Boolean,
    isGridOn: Boolean,
    language: AppLanguage,
    onZoomChange: (Float) -> Unit,
    onExposureChange: (Float) -> Unit,
    onStackingFramesChange: (Int) -> Unit,
    onTimerToggle: () -> Unit,
    onTorchToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onSwitchLens: () -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showExposureSlider by remember { mutableStateOf(false) }
    var showStackingPicker by remember { mutableStateOf(false) }

    val zoomPresets = listOf(1f, 2f, 5f, 10f, 15f, 20f, 30f, 40f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ObsidianBackground.copy(alpha = 0.88f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Secondary Drawer: EV Slider or Stacking Frame Picker
        AnimatedVisibility(visible = showExposureSlider) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${AppStrings.get("exposure", language)}: ${if (exposureCompensation >= 0) "+" else ""}${String.format("%.1f", exposureCompensation)} EV",
                        color = LunarSilver,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (exposureCompensation < -1.0f) "Moon Low Light" else "Standard",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
                Slider(
                    value = exposureCompensation,
                    onValueChange = onExposureChange,
                    valueRange = -4.0f..4.0f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = CosmicBorder
                    ),
                    modifier = Modifier.testTag("exposure_slider")
                )
            }
        }

        AnimatedVisibility(visible = showStackingPicker && captureMode == CaptureMode.MOON_PRO) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${AppStrings.get("stack_frames", language)}:",
                    color = LunarSilver,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                listOf(5, 10, 20, 30).forEach { count ->
                    val isSelected = count == stackingFrames
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ElectricViolet else GraphiteSurface)
                            .clickable { onStackingFramesChange(count) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("stacking_frame_$count")
                    ) {
                        Text(
                            text = "${count}x",
                            color = if (isSelected) Color.White else LunarSilver,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Zoom System: Discrete Pills & Digital Crop Disclosure
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isDigitalCrop = currentZoom > maxOpticalZoom
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${String.format("%.1f", currentZoom)}x",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (isDigitalCrop) AmberHighlight.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isDigitalCrop) AppStrings.get("digital_crop", language) else AppStrings.get("optical_zoom", language),
                        color = if (isDigitalCrop) AmberHighlight else NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(zoomPresets) { zoomVal ->
                    val isSelected = kotlin.math.abs(currentZoom - zoomVal) < 0.3f
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) NeonCyan else CosmicSurfaceVariant)
                            .border(1.dp, if (isSelected) NeonCyan else CosmicBorder, CircleShape)
                            .clickable { onZoomChange(zoomVal) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("zoom_preset_${zoomVal.toInt()}")
                    ) {
                        Text(
                            text = "${zoomVal.toInt()}x",
                            color = if (isSelected) DeepSpaceBlack else LunarSilver,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Quick Control Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showExposureSlider = !showExposureSlider },
                modifier = Modifier.testTag("btn_toggle_exposure")
            ) {
                Icon(
                    imageVector = Icons.Default.Brightness6,
                    contentDescription = "Exposure",
                    tint = if (showExposureSlider) NeonCyan else LunarSilver
                )
            }

            if (captureMode == CaptureMode.MOON_PRO) {
                IconButton(
                    onClick = { showStackingPicker = !showStackingPicker },
                    modifier = Modifier.testTag("btn_toggle_stacking")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Stacking",
                        tint = if (showStackingPicker) ElectricViolet else LunarSilver
                    )
                }
            }

            IconButton(
                onClick = onTimerToggle,
                modifier = Modifier.testTag("btn_toggle_timer")
            ) {
                Box {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = if (timerSeconds > 0) AmberHighlight else LunarSilver
                    )
                    if (timerSeconds > 0) {
                        Text(
                            text = "${timerSeconds}s",
                            color = AmberHighlight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }
            }

            IconButton(
                onClick = onGridToggle,
                modifier = Modifier.testTag("btn_toggle_grid")
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Grid",
                    tint = if (isGridOn) NeonCyan else LunarSilver
                )
            }

            IconButton(
                onClick = onTorchToggle,
                modifier = Modifier.testTag("btn_toggle_torch")
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "Torch",
                    tint = if (isTorchOn) AmberHighlight else LunarSilver
                )
            }

            IconButton(
                onClick = onSwitchLens,
                modifier = Modifier.testTag("btn_switch_lens")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = LunarSilver
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Primary Shutter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(3.5.dp, if (captureMode == CaptureMode.MOON_PRO) NeonCyan else Color.White, CircleShape)
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(if (captureMode == CaptureMode.MOON_PRO) NeonCyan else Color.White)
                    .clickable { onCaptureClick() }
                    .testTag("shutter_button"),
                contentAlignment = Alignment.Center
            ) {
                if (captureMode == CaptureMode.MOON_PRO) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MOON",
                            color = DeepSpaceBlack,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${stackingFrames}F",
                            color = DeepSpaceBlack,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
