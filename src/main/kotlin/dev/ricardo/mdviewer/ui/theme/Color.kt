package dev.ricardo.mdviewer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF000000)
val OnPrimary = Color(0xFFFFFFFF)
val BodyText = Color(0xFF5E5E5E)
val Mute = Color(0xFFAFAFAF)
val HairlineMid = Color(0xFF4B4B4B)
val Canvas = Color(0xFFFFFFFF)
val CanvasSoft = Color(0xFFEFEFEF)
val CanvasSofter = Color(0xFFF3F3F3)
val SurfacePressed = Color(0xFFE2E2E2)
val LinkBlue = Color(0xFF0000EE)
val BlackElevated = Color(0xFF282828)
val HighlightYellow = Color(0xFFFFE066)

data class HighlightColor(val name: String, val hex: String)

val HighlightColors = listOf(
    HighlightColor("Yellow", "#FFE8A3"),
    HighlightColor("Peach", "#FFD5C8"),
    HighlightColor("Pink", "#F8C9DC"),
    HighlightColor("Lavender", "#E4D4F7"),
    HighlightColor("Blue", "#C9E4FF"),
    HighlightColor("Mint", "#C9EED9"),
    HighlightColor("Lime", "#D7F0C4"),
)

val DarkCanvas = Color(0xFF000000)
val DarkSurface = Color(0xFF141414)
val DarkSurfaceVariant = Color(0xFF1C1C1C)
val DarkOnSurface = Color(0xFFFFFFFF)
val DarkOnSurfaceVariant = Color(0xFFB5B5B5)
val DarkOutline = Color(0xFF2A2A2A)
val DarkHighlight = Color(0x80FFE066)
val DarkElevated = Color(0xFF2A2A2A)

/**
 * Extended tokens that Material 3's [androidx.compose.material3.ColorScheme] does not carry.
 * Mirrors the Uber DESIGN.md color roles.
 */
@Immutable
data class UberColors(
    val canvas: Color,
    val canvasSoft: Color,
    val canvasSofter: Color,
    val ink: Color,
    val body: Color,
    val mute: Color,
    val hairlineMid: Color,
    val surfacePressed: Color,
    val blackElevated: Color,
    val onDark: Color,
    val accentHighlight: Color,
    val link: Color,
    val isLight: Boolean,
)

val LightUberColors = UberColors(
    canvas = Canvas,
    canvasSoft = CanvasSoft,
    canvasSofter = CanvasSofter,
    ink = Ink,
    body = BodyText,
    mute = Mute,
    hairlineMid = HairlineMid,
    surfacePressed = SurfacePressed,
    blackElevated = BlackElevated,
    onDark = OnPrimary,
    accentHighlight = HighlightYellow,
    link = LinkBlue,
    isLight = true,
)

val DarkUberColors = UberColors(
    canvas = DarkCanvas,
    canvasSoft = DarkSurfaceVariant,
    canvasSofter = DarkSurface,
    ink = DarkOnSurface,
    body = DarkOnSurfaceVariant,
    mute = Mute,
    hairlineMid = DarkOnSurfaceVariant,
    surfacePressed = DarkElevated,
    blackElevated = BlackElevated,
    onDark = DarkOnSurface,
    accentHighlight = DarkHighlight,
    link = Color(0xFF8AB4F8),
    isLight = false,
)

val LocalUberColors = staticCompositionLocalOf { LightUberColors }
