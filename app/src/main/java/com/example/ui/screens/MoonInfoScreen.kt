package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astronomy.MoonCalculator
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.DeepSpaceBlack
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.LunarSilver
import com.example.ui.theme.LunarSilverMuted
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBackground

@Composable
fun MoonInfoScreen(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val moonInfo = remember { MoonCalculator.calculateMoonPhase() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("moon_info_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.get("moon_phase_title", language),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "জ্যোতির্বৈজ্ঞানিক গণনা ও ফটোগ্রাফি নির্দেশিকা",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.NightsStay,
                    contentDescription = "Moon",
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Animated / Canvas Moon Phase Representation
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(DeepSpaceBlack)
                    .border(2.dp, CosmicBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Draw dark base moon
                    drawCircle(Color(0xFF1E2433), radius, center)

                    // Draw illuminated fraction
                    val illFraction = (moonInfo.illuminationPercent / 100f).coerceIn(0f, 1f)
                    val illumColor = Color(0xFFF1F5F9)

                    // Simple realistic phase rendering using arc/oval
                    if (illFraction > 0.05f) {
                        drawCircle(illumColor.copy(alpha = illFraction), radius, center)
                        // Lunar craters decorative spots
                        drawCircle(Color(0xFF94A3B8).copy(alpha = 0.4f), radius * 0.18f, Offset(center.x - radius * 0.25f, center.y - radius * 0.2f))
                        drawCircle(Color(0xFF94A3B8).copy(alpha = 0.35f), radius * 0.25f, Offset(center.x + radius * 0.2f, center.y + radius * 0.1f))
                        drawCircle(Color(0xFF94A3B8).copy(alpha = 0.3f), radius * 0.15f, Offset(center.x - radius * 0.1f, center.y + radius * 0.35f))
                    }

                    // Outer glowing rim
                    drawCircle(NeonCyan.copy(alpha = 0.4f), radius, center, style = Stroke(2.dp.toPx()))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phase Name
            Text(
                text = if (language == AppLanguage.BANGLA) moonInfo.phaseNameBn else moonInfo.phaseNameEn,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Key Astronomy Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = AppStrings.get("illumination", language),
                    value = "${String.format("%.1f", moonInfo.illuminationPercent)}%",
                    subtitle = "দৃশ্যমান অংশ",
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = AppStrings.get("lunar_age", language),
                    value = "${String.format("%.1f", moonInfo.ageDays)} দিন",
                    subtitle = "সিনোডিক চক্র",
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "দূরত্ব",
                    value = "${moonInfo.moonDistanceKm / 1000}k km",
                    subtitle = "পৃথিবী থেকে",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Photography Tips Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Tips",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("photo_tips", language),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (language == AppLanguage.BANGLA) moonInfo.photographyTipsBn else moonInfo.photographyTipsEn,
                        color = LunarSilver,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GraphiteSurface, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "💡 পরামর্শ: চাঁদের ছবি তোলার সময় সর্বদা এক্সপোজার ভ্যালু (EV) কমিয়ে রাখুন। পূর্ণিমা বা উজ্জ্বল চাঁদের ক্ষেত্রে EV -২.০ থেকে -৩.০ রাখলে চাঁদের গর্ত ও পৃষ্ঠের বিস্তারিত দৃশ্যমান হয়।",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Astronomical Methodology Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = LunarSilverMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "চাঁদের অবস্থান ও দশা নির্ভরযোগ্য সিনোডিক চক্র সূত্রের মাধ্যমে হিসাবকৃত। এটি কৃত্রিম পর্যবেক্ষণ নয়।",
                    color = LunarSilverMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        modifier = modifier.border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = LunarSilverMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color(0xFF64748B), fontSize = 9.sp)
        }
    }
}
