package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SonicForgeColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBg,
    primaryContainer = ElectricCyanDim,
    onPrimaryContainer = TextPrimary,
    secondary = AmberGold,
    onSecondary = ObsidianBg,
    secondaryContainer = AmberGoldDim,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldNeon,
    onTertiary = ObsidianBg,
    tertiaryContainer = EmeraldDim,
    onTertiaryContainer = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = CrimsonRecord,
    outline = BorderSubtle
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SonicForgeColorScheme,
        typography = Typography,
        shapes = StudioShapes,
        content = content
    )
}
