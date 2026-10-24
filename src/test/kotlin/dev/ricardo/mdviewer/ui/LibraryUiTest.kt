package dev.ricardo.mdviewer.ui

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import dev.ricardo.mdviewer.di.AppGraph
import dev.ricardo.mdviewer.feature.library.LibraryScreen
import dev.ricardo.mdviewer.feature.library.LibraryViewModel
import dev.ricardo.mdviewer.ui.theme.MdViewerTheme
import org.junit.Test
import java.io.File

@OptIn(ExperimentalTestApi::class)
class LibraryUiTest {

    private fun tempDir(): File = File.createTempFile("library-ui", "").apply {
        delete()
        mkdirs()
    }

    @Test
    fun `prompts for a library folder when none is configured`() = runComposeUiTest {
        val dir = tempDir()
        try {
            val graph = AppGraph(File(dir, "config"), File(dir, "data"))
            setContent {
                val scope = rememberCoroutineScope()
                val viewModel = remember {
                    LibraryViewModel(
                        settingsRepository = graph.settingsRepository,
                        documentRepository = graph.documentRepository,
                        markdownIndex = graph.markdownIndex,
                        scope = scope,
                    )
                }
                MdViewerTheme(darkTheme = false) {
                    LibraryScreen(viewModel, {}, {}, {}, {})
                }
            }

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("Choose a library folder").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Open a file").assertExists()
            onNodeWithText("Choose folder").assertExists()
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `lists markdown files from the configured root`() = runComposeUiTest {
        val dir = tempDir()
        try {
            val library = File(dir, "notes").apply { mkdirs() }
            File(library, "alpha.md").writeText("# Alpha")
            File(library, "sub").mkdirs()
            File(library, "sub/beta.md").writeText("# Beta")

            val graph = AppGraph(File(dir, "config"), File(dir, "data"))
            graph.settingsRepository.setLibraryRoot(library.absolutePath)

            setContent {
                val scope = rememberCoroutineScope()
                val viewModel = remember {
                    LibraryViewModel(
                        settingsRepository = graph.settingsRepository,
                        documentRepository = graph.documentRepository,
                        markdownIndex = graph.markdownIndex,
                        scope = scope,
                    )
                }
                MdViewerTheme(darkTheme = false) {
                    LibraryScreen(viewModel, {}, {}, {}, {})
                }
            }

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("alpha.md").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("sub").assertExists()
            onNodeWithText("New note").assertExists()
        } finally {
            dir.deleteRecursively()
        }
    }
}
