package dev.ricardo.mdviewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

internal val Color_Error = androidx.compose.ui.graphics.Color(0xFFD32F2F)

private val LightScheme = lightColorScheme(
    primary = Ink,
    onPrimary = OnPrimary,
    primaryContainer = CanvasSoft,
    onPrimaryContainer = Ink,
    secondary = Ink,
    onSecondary = OnPrimary,
    background = Canvas,
    onBackground = Ink,
    surface = Canvas,
    onSurface = Ink,
    surfaceVariant = CanvasSoft,
    onSurfaceVariant = BodyText,
    outline = SurfacePressed,
    outlineVariant = CanvasSoft,
    error = Color_Error,
    onError = OnPrimary,
)

private val DarkScheme = darkColorScheme(
    primary = DarkOnSurface,
    onPrimary = DarkCanvas,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkOnSurface,
    secondary = DarkOnSurface,
    onSecondary = DarkCanvas,
    background = DarkCanvas,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceVariant,
    error = Color_Error,
    onError = OnPrimary,
)

@Composable
fun MdViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val uberColors = if (darkTheme) DarkUberColors else LightUberColors
    CompositionLocalProvider(
        LocalUberColors provides uberColors,
        LocalSpacing provides UberSpacing(),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = UberTypography,
            shapes = UberShapes,
            content = content,
        )
    }
}
