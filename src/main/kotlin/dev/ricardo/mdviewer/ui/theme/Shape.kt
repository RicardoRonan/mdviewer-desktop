package dev.ricardo.mdviewer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Signature pill used on every interactive element. */
val PillShape = RoundedCornerShape(999.dp)

/** The off-shape 36px tab toggle from the ride-request form. */
val TabPillShape = RoundedCornerShape(36.dp)

val UberShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp),
)
