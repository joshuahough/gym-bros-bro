package com.gymbrosbro.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Size of one "pixel" in the chunky UI chrome (borders, notches, button depth). */
object PixelDimens {
    val unit = 3.dp
}

private val GbbColorScheme = darkColorScheme(
    primary = GbbColors.Gold,
    onPrimary = GbbColors.Void,
    secondary = GbbColors.Cyan,
    onSecondary = GbbColors.Void,
    tertiary = GbbColors.Pink,
    onTertiary = GbbColors.Void,
    background = GbbColors.Void,
    onBackground = GbbColors.Chalk,
    surface = GbbColors.Night,
    onSurface = GbbColors.Chalk,
    surfaceVariant = GbbColors.Dusk,
    // Menus, date pickers and dialogs read these container colors.
    surfaceContainerLowest = GbbColors.Void,
    surfaceContainerLow = GbbColors.Night,
    surfaceContainer = GbbColors.Dusk,
    surfaceContainerHigh = GbbColors.Dusk,
    surfaceContainerHighest = GbbColors.Steel,
    onSurfaceVariant = GbbColors.Fog,
    outline = GbbColors.Steel,
    error = GbbColors.Red,
    onError = GbbColors.Void,
)

// Square everything; pixel corners come from PixelPanel/PixelButton, not rounded shapes.
private val Square = RoundedCornerShape(0.dp)
private val GbbShapes = Shapes(
    extraSmall = Square,
    small = Square,
    medium = Square,
    large = Square,
    extraLarge = Square,
)

@Composable
fun GymBrosBroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GbbColorScheme,
        typography = GbbTypography,
        shapes = GbbShapes,
        content = content,
    )
}
