package dev.ricardo.mdviewer.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dev.ricardo.mdviewer.di.AppGraph
import dev.ricardo.mdviewer.feature.reader.ReaderScreen
import dev.ricardo.mdviewer.ui.theme.MdViewerTheme
import org.junit.Test
import java.io.File

@OptIn(ExperimentalTestApi::class)
class ReaderUiTest {

    private fun tempDir(): File = File.createTempFile("reader-ui", "").apply {
        delete()
        mkdirs()
    }

    @Test
    fun `opens a markdown file and renders its blocks`() = runComposeUiTest {
        val dir = tempDir()
        try {
            val file = File(dir, "welcome.md").apply {
                writeText(
                    """
                    # Welcome aboard

                    A paragraph with a [link](https://example.com) and `code`.

                    - first item
                    - second item
                    """.trimIndent(),
                )
            }
            val graph = AppGraph(File(dir, "config"), File(dir, "data"))

            setContent {
                MdViewerTheme(darkTheme = false) {
                    ReaderScreen(
                        path = file.absolutePath,
                        startInEditor = false,
                        graph = graph,
                        onBack = {},
                        onOpenNotebook = {},
                    )
                }
            }

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("welcome.md").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("welcome.md").assertExists()
            onNodeWithText("Welcome aboard", substring = true).assertExists()
            onNodeWithText("first item", substring = true).assertExists()
        } finally {
            dir.deleteRecursively()
        }
    }
}
