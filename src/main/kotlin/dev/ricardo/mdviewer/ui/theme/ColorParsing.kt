package dev.ricardo.mdviewer.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

fun parseHexColor(hex: String, fallback: Color): Color {
    val cleaned = hex.trim().removePrefix("#")
    if (cleaned.isEmpty()) return fallback
    return runCatching {
        when (cleaned.length) {
            3 -> {
                val expanded = cleaned.map { "$it$it" }.joinToString("")
                Color(0xFF000000 or expanded.toLong(16))
            }
            6 -> Color(0xFF000000 or cleaned.toLong(16))
            8 -> Color(cleaned.toLong(16))
            else -> fallback
        }
    }.getOrDefault(fallback)
}

fun Color.toHexString(): String = String.format("#%06X", toArgbInt() and 0xFFFFFF)

fun Color.toArgbInt(): Int =
    ((alpha * 255).roundToInt() shl 24) or
        ((red * 255).roundToInt() shl 16) or
        ((green * 255).roundToInt() shl 8) or
        (blue * 255).roundToInt()
