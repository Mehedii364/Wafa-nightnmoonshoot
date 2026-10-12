package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepSpaceBlack,
    primaryContainer = CosmicSurfaceVariant,
    onPrimaryContainer = LunarSilver,
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = CosmicSurfaceVariant,
    onSecondaryContainer = LunarSilver,
    tertiary = AmberHighlight,
    onTertiary = DeepSpaceBlack,
    background = ObsidianBackground,
    onBackground = LunarSilver,
    surface = GraphiteSurface,
    onSurface = LunarSilver,
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = LunarSilverMuted,
    outline = CosmicBorder,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
