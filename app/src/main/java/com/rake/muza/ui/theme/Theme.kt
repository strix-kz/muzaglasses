package com.rake.muza.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MuzaDarkColorScheme = darkColorScheme(
    primary = MuzaColors.BluePrimary,
    onPrimary = MuzaColors.TextPrimary,
    primaryContainer = MuzaColors.BlueDeep,
    onPrimaryContainer = MuzaColors.TextPrimary,
    secondary = MuzaColors.BlueGlow,
    onSecondary = MuzaColors.Background,
    secondaryContainer = MuzaColors.SurfaceHighlight,
    onSecondaryContainer = MuzaColors.TextPrimary,
    tertiary = MuzaColors.EmeraldSoft,
    onTertiary = MuzaColors.Background,
    background = MuzaColors.Background,
    onBackground = MuzaColors.TextPrimary,
    surface = MuzaColors.Surface,
    onSurface = MuzaColors.TextPrimary,
    surfaceVariant = MuzaColors.SurfaceElevated,
    onSurfaceVariant = MuzaColors.TextSecondary,
    outline = MuzaColors.Border,
    error = MuzaColors.Danger,
    onError = MuzaColors.TextPrimary
)

private val MuzaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MuzaGlassesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MuzaDarkColorScheme,
        typography = MuzaTypography,
        shapes = MuzaShapes,
        content = content
    )
}
