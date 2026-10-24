package dev.ricardo.mdviewer.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Feather icons (MIT), drawn as 24px strokes so the toolbar is not stuck with fallback glyphs.
 * https://github.com/feathericons/feather
 */
object Feather {
    val ArrowLeft: ImageVector = icon("ArrowLeft") {
        stroke("M19 12H5")
        stroke("M12 19l-7-7 7-7")
    }

    val BookOpen: ImageVector = icon("BookOpen") {
        stroke("M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z")
        stroke("M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z")
    }

    val Settings: ImageVector = icon("Settings") {
        stroke("M12 9a3 3 0 1 0 0 6 3 3 0 1 0 0-6z")
        stroke(
            "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0" +
                "l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2" +
                " 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06" +
                "a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82" +
                " 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9" +
                "a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06" +
                "a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2" +
                "v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0" +
                " 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21" +
                "a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z",
        )
    }

    val Folder: ImageVector = icon("Folder") {
        stroke("M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z")
    }

    val File: ImageVector = icon("File") {
        stroke("M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z")
        stroke("M13 2v7h7")
    }

    val ChevronRight: ImageVector = icon("ChevronRight") {
        stroke("M9 18l6-6-6-6")
    }

    val Edit: ImageVector = icon("Edit") {
        stroke("M12 20h9")
        stroke("M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z")
    }

    val Eye: ImageVector = icon("Eye") {
        stroke("M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z")
        stroke("M12 9a3 3 0 1 0 0 6 3 3 0 1 0 0-6z")
    }

    val Check: ImageVector = icon("Check") {
        stroke("M20 6L9 17l-5-5")
    }

    val Plus: ImageVector = icon("Plus") {
        stroke("M12 5v14")
        stroke("M5 12h14")
    }

    val More: ImageVector = icon("More") {
        stroke("M12 8a1 1 0 1 0 0-2 1 1 0 0 0 0 2z")
        stroke("M12 13a1 1 0 1 0 0-2 1 1 0 0 0 0 2z")
        stroke("M12 18a1 1 0 1 0 0-2 1 1 0 0 0 0 2z")
    }

    val Trash: ImageVector = icon("Trash") {
        stroke("M3 6h18")
        stroke("M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2")
        stroke("M10 11v6")
        stroke("M14 11v6")
    }
}

private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

private fun ImageVector.Builder.stroke(data: String) {
    addPath(
        pathData = PathParser().parsePathString(data).toNodes(),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}
