package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val RealityOsDarkColorScheme = darkColorScheme(
    primary = SystemColors.ElectricCyan,
    onPrimary = SystemColors.ObsidianBlack,
    primaryContainer = SystemColors.PanelSurfaceElevated,
    onPrimaryContainer = SystemColors.ElectricCyan,
    secondary = SystemColors.ArcaneViolet,
    onSecondary = SystemColors.PureIce,
    secondaryContainer = ColorTokens.VioletContainer,
    onSecondaryContainer = SystemColors.PureIce,
    tertiary = SystemColors.SystemBlue,
    onTertiary = SystemColors.PureIce,
    background = SystemColors.ObsidianBlack,
    onBackground = SystemColors.PureIce,
    surface = SystemColors.DeepNavy,
    onSurface = SystemColors.PureIce,
    surfaceVariant = SystemColors.PanelSurface,
    onSurfaceVariant = SystemColors.MetallicSilver,
    outline = SystemColors.PanelBorderCyan,
    outlineVariant = SystemColors.SubtleGridLine,
    error = SystemColors.DangerCrimson,
    onError = SystemColors.PureIce
)

private object ColorTokens {
    val VioletContainer = androidx.compose.ui.graphics.Color(0xFF231842)
}

val RealityOsShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp)
)

val LocalHudGlowEnabled = staticCompositionLocalOf { true }

@Composable
fun SoloLevelingTheme(
    hudGlowEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalHudGlowEnabled provides hudGlowEnabled) {
        MaterialTheme(
            colorScheme = RealityOsDarkColorScheme,
            typography = RealityOsTypography,
            shapes = RealityOsShapes,
            content = content
        )
    }
}
