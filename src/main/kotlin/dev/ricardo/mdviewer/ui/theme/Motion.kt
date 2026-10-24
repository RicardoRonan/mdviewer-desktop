package dev.ricardo.mdviewer.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Short ease-out motion. Durations stay under a third of a second so screens feel immediate. */
object Motion {
    const val Quick = 180
    const val Base = 280
    val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
}

/**
 * Desktop has no animator-duration scale, so motion stays on unless the app is started with
 * `-Dmdviewer.reduceMotion=true`.
 */
@Composable
fun rememberMotionEnabled(): Boolean = remember {
    System.getProperty("mdviewer.reduceMotion") != "true"
}

fun <T> motionTween(durationMillis: Int, enabled: Boolean): FiniteAnimationSpec<T> =
    if (enabled) tween(durationMillis = durationMillis, easing = Motion.EaseOut) else snap()

/** Presses settle by a hair instead of snapping. */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val motion = rememberMotionEnabled()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && motion) 0.97f else 1f,
        animationSpec = motionTween(Motion.Quick, motion),
        label = "press",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
