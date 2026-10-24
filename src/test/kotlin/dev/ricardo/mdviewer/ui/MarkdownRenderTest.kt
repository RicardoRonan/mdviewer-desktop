package dev.ricardo.mdviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.ricardo.mdviewer.domain.markdown.MarkdownParser
import dev.ricardo.mdviewer.feature.reader.MarkdownContent
import dev.ricardo.mdviewer.ui.theme.MdViewerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

/**
 * Paints the reader renderer off-screen through Skia and checks real pixels. This catches blank
 * windows and renderer regressions without needing a display server.
 */
@OptIn(ExperimentalComposeUiApi::class)
class MarkdownRenderTest {

    @Test
    fun `renders a document to a non-blank png`() {
        val source = """
            # Rendering check

            A paragraph with **bold**, `inline code`, and a [link](https://example.com).

            > A quoted line of text.

            - one
            - two

            ```kotlin
            fun main() = println("hi")
            ```

            | Column | Value |
            | --- | --- |
            | alpha | 1 |
        """.trimIndent()
        val blocks = MarkdownParser.parse(source)

        val scene = ImageComposeScene(width = 900, height = 760, density = Density(1f)) {
            MdViewerTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {
                    MarkdownContent(
                        blocks = blocks,
                        highlights = emptyMap(),
                        fontScale = 1f,
                        loadRemoteImages = false,
                        clearSelection = true,
                        onSelectionChange = { _, _ -> },
                        onLinkClick = {},
                    )
                }
            }
        }

        try {
            val png = scene.render().encodeToData()?.bytes
                ?: error("Skia returned no image data")
            val output = File("build/reports/ui/markdown-render.png")
            output.parentFile?.mkdirs()
            output.writeBytes(png)

            val image = ImageIO.read(output)
            assertEquals(900, image.width)
            assertEquals(760, image.height)

            val colors = mutableSetOf<Int>()
            var step = 0
            while (step < image.width) {
                var row = 0
                while (row < image.height) {
                    colors += image.getRGB(step, row)
                    row += 7
                }
                step += 7
            }
            assertTrue("expected rendered text pixels, found ${colors.size} colors", colors.size > 10)
        } finally {
            scene.close()
        }
    }
}
