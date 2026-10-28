package dev.ricardo.mdviewer.feature.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class NoteTooltipUiTest {

    private val noteRange = TextHighlight(0, 5, Color.Yellow, note = "Check this", tags = listOf("todo"))
    private val plainHighlight = TextHighlight(0, 5, Color.Yellow)

    @Test
    fun `note lookup only matches ranges that carry note text`() {
        val notes = listOf(noteRange, TextHighlight(10, 14, Color.Yellow, note = "Second note"))
        assertEquals("Check this", noteUnder(0, notes)!!.note)
        assertEquals("Check this", noteUnder(4, notes)!!.note)
        assertNull("the end offset belongs to the next range", noteUnder(5, notes))
        assertEquals("Second note", noteUnder(10, notes)!!.note)
        assertNull("offsets inside plain highlights stay silent", noteUnder(7, notes))
    }

    @Test
    fun `hovering a noted highlight shows its note`() = runComposeUiTest {
        setContent {
            Column {
                SelectableMarkdownBlock(
                    text = AnnotatedString("hello world"),
                    style = TextStyle(fontSize = 32.sp),
                    clearSelection = false,
                    onSelectionChange = {},
                    onLinkClick = {},
                    notes = listOf(noteRange),
                )
            }
        }

        onNodeWithText("hello world").performMouseInput { moveTo(Offset(8f, 8f)) }
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Check this").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "tags should ride along with the note",
            onAllNodesWithText("#todo").fetchSemanticsNodes().isNotEmpty(),
        )
    }

    @Test
    fun `escape dismisses the note card`() = runComposeUiTest {
        setContent {
            Column {
                SelectableMarkdownBlock(
                    text = AnnotatedString("hello world"),
                    style = TextStyle(fontSize = 32.sp),
                    clearSelection = false,
                    onSelectionChange = {},
                    onLinkClick = {},
                    notes = listOf(noteRange),
                )
            }
        }

        onNodeWithText("hello world").performMouseInput { moveTo(Offset(8f, 8f)) }
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Check this").fetchSemanticsNodes().isNotEmpty()
        }

        onNodeWithText("hello world").performSemanticsAction(SemanticsActions.RequestFocus)
        onNodeWithText("hello world").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        onNodeWithText("Check this").assertDoesNotExist()
    }

    @Test
    fun `plain highlights show no card on hover`() = runComposeUiTest {
        setContent {
            Column {
                SelectableMarkdownBlock(
                    text = AnnotatedString("hello world"),
                    style = TextStyle(fontSize = 32.sp),
                    clearSelection = false,
                    onSelectionChange = {},
                    onLinkClick = {},
                    notes = listOf(plainHighlight),
                )
            }
        }

        onNodeWithText("hello world").performMouseInput { moveTo(Offset(8f, 8f)) }
        Thread.sleep(600)
        waitForIdle()
        onNodeWithText("Check this").assertDoesNotExist()
        onNodeWithText("hello world").performClick()
        waitForIdle()
    }
}
