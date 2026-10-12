package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.HardwareDiscoveryService
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.model.CapabilityState
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeepSpaceBlack
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.LunarSilver
import com.example.ui.theme.LunarSilverMuted
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBackground

@Composable
fun HardwareReportScreen(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val report = remember { HardwareDiscoveryService.inspectHardware(context) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("hardware_report_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.get("hw_report_title", language),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = AppStrings.get("hw_target_device", language),
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Device",
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Status Banner
            val (statusColor, statusText) = when (report.overallStatus) {
                CapabilityState.SUPPORTED -> EmeraldSuccess to "SUPPORTED (সম্পূর্ণ সামঞ্জস্যপূর্ণ)"
                CapabilityState.LIMITED -> AmberHighlight to "LIMITED (সীমিত হার্ডওয়্যার স্তর)"
                CapabilityState.UNSUPPORTED -> CrimsonAlert to "UNSUPPORTED (অসমর্থিত)"
                CapabilityState.PERMISSION_REQUIRED -> AmberHighlight to "PERMISSION REQUIRED (অনুমতি প্রয়োজন)"
                CapabilityState.CAMERA_BUSY -> AmberHighlight to "CAMERA BUSY (ব্যস্ত)"
                CapabilityState.TEMPORARILY_UNAVAILABLE -> AmberHighlight to "TEMPORARILY UNAVAILABLE"
                CapabilityState.ERROR -> CrimsonAlert to "ERROR"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, statusColor, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (report.overallStatus == CapabilityState.SUPPORTED) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Status",
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = report.explanationMessage,
                            color = LunarSilver,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detected Device Info Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("শনাক্তকৃত ডিভাইস স্পেসিফিকেশন", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    HardwareSpecRow("মডেল", report.detectedDeviceModel)
                    HardwareSpecRow("প্রস্তুতকারক", report.detectedManufacturer)
                    HardwareSpecRow("লক্ষ্য মডেল (CPH2701) মিল", if (report.isTargetModelMatched) "হ্যাঁ (OPPO Reno13 F 4G)" else "না (অন্যান্য অ্যান্ড্রয়েড মডেল)")
                    HardwareSpecRow("সর্বোচ্চ জুম ক্ষমতা", "${String.format("%.1f", report.maxZoomAvailable)}x")
                    HardwareSpecRow("অপটিক্যাল স্ট্যাবিলাইজেশন (OIS)", if (report.hasOpticalStabilization) "উপলব্ধ (Hardware OIS)" else "উপলব্ধ নয় (EIS বা ট্রাইপড প্রস্তাবিত)")
                    HardwareSpecRow("RAW / DNG ক্যাপচার", if (report.isRawSupported) "সমর্থিত" else "উপলব্ধ নয় (JPEG স্ট্যাকিং সক্রিয়)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Physical Cameras Details
            Text(
                text = "শারীরিক ক্যামেরা মডিউলসমূহ (${report.physicalCameras.size} টি)",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            for (cam in report.physicalCameras) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Camera ID: ${cam.cameraId}", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(cam.lensFacing, color = LunarSilverMuted, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HardwareSpecRow("হার্ডওয়্যার স্তর", cam.hardwareLevel)
                        HardwareSpecRow("ফোকাল দৈর্ঘ্য", cam.focalLengths.joinToString(", ") { "${it}mm" })
                        HardwareSpecRow("ম্যাক্স ডিজিটাল জুম", "${cam.maxDigitalZoom}x")
                        HardwareSpecRow("সমর্থিত AF মোডসমূহ", cam.afModes.take(3).joinToString(", "))
                    }
                }
            }
        }
    }
}

@Composable
private fun HardwareSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = LunarSilverMuted, fontSize = 11.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}
