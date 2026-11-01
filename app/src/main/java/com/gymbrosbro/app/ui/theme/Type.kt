package com.gymbrosbro.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gymbrosbro.app.R

/** Press Start 2P (SIL OFL 1.1): headers, big numbers, buttons, celebrations. */
val PixelFont = FontFamily(Font(R.font.press_start_2p))

/** System sans for anything read mid-set: lists, inputs, body copy. */
val BodyFont = FontFamily.Default

private fun pixel(size: Int, line: Int) =
    TextStyle(fontFamily = PixelFont, fontSize = size.sp, lineHeight = line.sp)

private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = BodyFont, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

val GbbTypography = Typography(
    displayLarge = pixel(28, 38),
    displayMedium = pixel(22, 30),
    displaySmall = pixel(18, 26),
    headlineLarge = pixel(18, 26),
    headlineMedium = pixel(16, 24),
    headlineSmall = pixel(14, 22),
    titleLarge = pixel(12, 20),
    titleMedium = body(16, 22, FontWeight.Bold),
    titleSmall = body(14, 20, FontWeight.Bold),
    bodyLarge = body(16, 24),
    bodyMedium = body(14, 20),
    bodySmall = body(12, 16),
    labelLarge = pixel(12, 16),
    labelMedium = body(12, 16, FontWeight.Bold),
    labelSmall = body(11, 14, FontWeight.Medium),
)
