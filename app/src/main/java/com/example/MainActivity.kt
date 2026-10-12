package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.preferences.UserPreferences
import com.example.gallery.GalleryRepository
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.HardwareReportScreen
import com.example.ui.screens.MoonInfoScreen
import com.example.ui.screens.PngStudioScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WatermarkStudioScreen
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicSurfaceVariant
import com.example.ui.theme.DeepSpaceBlack
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.LunarSilver
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBackground

enum class ScreenTab {
    CAMERA,
    PNG_STUDIO,
    WATERMARK,
    GALLERY,
    MOON_INFO,
    SETTINGS,
    HARDWARE_REPORT
}

class MainActivity : ComponentActivity() {

    private lateinit var userPreferences: UserPreferences
    private lateinit var galleryRepository: GalleryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        userPreferences = UserPreferences(this)
        galleryRepository = GalleryRepository(this)

        setContent {
            MyApplicationTheme {
                MainAppNavHost(
                    userPreferences = userPreferences,
                    galleryRepository = galleryRepository
                )
            }
        }
    }
}

@Composable
fun MainAppNavHost(
    userPreferences: UserPreferences,
    galleryRepository: GalleryRepository
) {
    val language by userPreferences.language.collectAsState()
    var currentTab by remember { mutableStateOf(ScreenTab.CAMERA) }
    var selectedImagePathForStudio by remember { mutableStateOf<String?>(null) }
    var selectedImagePathForWatermark by remember { mutableStateOf<String?>(null) }

    // Hardware back button navigation handling
    if (currentTab != ScreenTab.CAMERA) {
        BackHandler {
            if (currentTab == ScreenTab.HARDWARE_REPORT) {
                currentTab = ScreenTab.SETTINGS
            } else {
                currentTab = ScreenTab.CAMERA
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBackground,
        bottomBar = {
            // Elegant M3 floating bottom navigation bar
            CustomBottomNavigationBar(
                currentTab = currentTab,
                language = language,
                onTabSelected = { tab ->
                    currentTab = tab
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    ScreenTab.CAMERA -> {
                        CameraScreen(
                            userPreferences = userPreferences,
                            galleryRepository = galleryRepository,
                            language = language,
                            onOpenPngStudio = { path ->
                                selectedImagePathForStudio = path
                                currentTab = ScreenTab.PNG_STUDIO
                            },
                            onOpenGallery = { currentTab = ScreenTab.GALLERY },
                            onOpenMoonInfo = { currentTab = ScreenTab.MOON_INFO },
                            onOpenSettings = { currentTab = ScreenTab.SETTINGS }
                        )
                    }

                    ScreenTab.PNG_STUDIO -> {
                        PngStudioScreen(
                            initialFilePath = selectedImagePathForStudio,
                            language = language,
                            onNavigateBack = { currentTab = ScreenTab.CAMERA }
                        )
                    }

                    ScreenTab.WATERMARK -> {
                        WatermarkStudioScreen(
                            initialFilePath = selectedImagePathForWatermark,
                            language = language
                        )
                    }

                    ScreenTab.GALLERY -> {
                        GalleryScreen(
                            repository = galleryRepository,
                            language = language,
                            onOpenInPngStudio = { path ->
                                selectedImagePathForStudio = path
                                currentTab = ScreenTab.PNG_STUDIO
                            },
                            onOpenInWatermarkStudio = { path ->
                                selectedImagePathForWatermark = path
                                currentTab = ScreenTab.WATERMARK
                            }
                        )
                    }

                    ScreenTab.MOON_INFO -> {
                        MoonInfoScreen(language = language)
                    }

                    ScreenTab.SETTINGS -> {
                        SettingsScreen(
                            userPreferences = userPreferences,
                            onNavigateToHardwareReport = { currentTab = ScreenTab.HARDWARE_REPORT }
                        )
                    }

                    ScreenTab.HARDWARE_REPORT -> {
                        HardwareReportScreen(language = language)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigationBar(
    currentTab: ScreenTab,
    language: AppLanguage,
    onTabSelected: (ScreenTab) -> Unit
) {
    data class NavItem(
        val tab: ScreenTab,
        val icon: ImageVector,
        val labelKey: String
    )

    val items = listOf(
        NavItem(ScreenTab.CAMERA, Icons.Default.CameraAlt, "tab_camera"),
        NavItem(ScreenTab.PNG_STUDIO, Icons.Default.AutoAwesome, "tab_png_studio"),
        NavItem(ScreenTab.WATERMARK, Icons.Default.Draw, "watermark_title"),
        NavItem(ScreenTab.GALLERY, Icons.Default.PhotoLibrary, "tab_gallery"),
        NavItem(ScreenTab.MOON_INFO, Icons.Default.NightsStay, "tab_moon_info"),
        NavItem(ScreenTab.SETTINGS, Icons.Default.Settings, "tab_settings")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSpaceBlack)
            .border(width = 0.5.dp, color = CosmicBorder)
            .navigationBarsPadding()
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag("bottom_nav_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentTab == item.tab ||
                        (currentTab == ScreenTab.HARDWARE_REPORT && item.tab == ScreenTab.SETTINGS)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(item.tab) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("nav_${item.tab.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.labelKey,
                            tint = if (isSelected) NeonCyan else LunarSilver,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (item.tab) {
                            ScreenTab.CAMERA -> AppStrings.get("tab_camera", language)
                            ScreenTab.PNG_STUDIO -> "পিএনজি"
                            ScreenTab.WATERMARK -> "চিহ্ন"
                            ScreenTab.GALLERY -> AppStrings.get("tab_gallery", language)
                            ScreenTab.MOON_INFO -> "তথ্য"
                            ScreenTab.SETTINGS -> AppStrings.get("tab_settings", language)
                            else -> ""
                        },
                        color = if (isSelected) NeonCyan else Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
