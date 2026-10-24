package dev.ricardo.mdviewer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Uber DESIGN.md type scale. UberMove/UberMoveText are proprietary; Inter (ss01) is the
 * documented open-source substitute. Bundling Inter is a follow-up; the default sans is used
 * until then, with the exact sizes/weights/line-heights preserved.
 */
private val Display = FontFamily.SansSerif
private val TextFace = FontFamily.SansSerif

val UberTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = TextFace, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = TextFace, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = TextFace, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = TextFace, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontFamily = TextFace, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = TextFace, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = TextFace, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = TextFace, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 20.sp),
)

// Named tokens for direct use in markdown rendering.
val MdBodyLarge = TextStyle(fontFamily = TextFace, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 26.sp)
val MdBodyMedium = TextStyle(fontFamily = TextFace, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp)
val MdCaption = TextStyle(fontFamily = TextFace, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 20.sp)
val MdCode = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 22.sp)
