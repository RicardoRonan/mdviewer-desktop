package dev.ricardo.mdviewer.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import dev.ricardo.mdviewer.di.AppGraph
import dev.ricardo.mdviewer.domain.model.Annotation
import dev.ricardo.mdviewer.domain.model.AnnotationKind
import dev.ricardo.mdviewer.feature.reader.ReaderScreen
import dev.ricardo.mdviewer.ui.theme.MdViewerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

@OptIn(ExperimentalTestApi::class)
class SelectionUiTest {

    private fun tempDir(): File = File.createTempFile("selection-ui", "").apply {
        delete()
        mkdirs()
    }

    @Test
    fun `selecting text opens the note sheet and saves a note`() = runComposeUiTest {
        val dir = tempDir()
        try {
            val file = File(dir, "doc.md").apply { writeText("Hello world paragraph.") }
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
                onAllNodesWithText("doc.md").fetchSemanticsNodes().isNotEmpty()
            }

            onNodeWithText("Hello world paragraph.").performTextInputSelection(TextRange(0, 5))
            waitForIdle()

            onNodeWithText("Note").assertExists()
            onNodeWithText("Note").performClick()
            waitForIdle()

            onNodeWithText("Add note").assertExists()
            onNodeWithText("Your note").performTextInput("my note")
            onNodeWithText("Save").performClick()
            waitForIdle()

            val saved = runBlocking { graph.annotationRepository.observeAll().first() }
            assertEquals(1, saved.size)
            assertEquals("my note", saved.single().note)
            assertEquals("Hello", saved.single().selector.exact)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `selecting text and picking a colour saves a highlight`() = runComposeUiTest {
        val dir = tempDir()
        try {
            val file = File(dir, "doc.md").apply { writeText("Hello world paragraph.") }
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
                onAllNodesWithText("doc.md").fetchSemanticsNodes().isNotEmpty()
            }

            onNodeWithText("Hello world paragraph.").performTextInputSelection(TextRange(0, 5))
            waitForIdle()

            onNodeWithContentDescription("Yellow highlight").performClick()

            var saved = emptyList<Annotation>()
            waitUntil(timeoutMillis = 10_000) {
                saved = runBlocking { graph.annotationRepository.observeAll().first() }
                saved.isNotEmpty()
            }
            assertEquals(AnnotationKind.HIGHLIGHT, saved.single().kind)
            assertEquals("#FFE8A3", saved.single().color)
            assertEquals("Hello", saved.single().selector.exact)
        } finally {
            dir.deleteRecursively()
        }
    }
}
