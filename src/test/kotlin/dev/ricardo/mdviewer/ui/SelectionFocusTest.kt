package dev.ricardo.mdviewer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.dragAndDrop
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import dev.ricardo.mdviewer.feature.reader.SelectableMarkdownBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SelectionFocusTest {

    /**
     * Regression: a text field collapses its own selection when focus leaves it. Reporting that
     * collapse would tear down the selection action bar the instant the reader presses a button on
     * it, so highlight / note / copy would never fire.
     */
    @Test
    fun `selection survives focus loss and only a tap dismisses it`() = runComposeUiTest {
        val reported = mutableListOf<IntRange>()
        setContent {
            Column {
                SelectableMarkdownBlock(
                    text = AnnotatedString("hello world"),
                    style = TextStyle(),
                    clearSelection = false,
                    onSelectionChange = { reported += it },
                    onLinkClick = {},
                )
                var other by remember { mutableStateOf(TextFieldValue("other")) }
                BasicTextField(value = other, onValueChange = { other = it })
            }
        }

        onNodeWithText("hello world").performTextInputSelection(TextRange(0, 5))
        waitForIdle()
        assertEquals(0..4, reported.last())

        onNodeWithText("other").performSemanticsAction(SemanticsActions.RequestFocus)
        waitForIdle()
        assertTrue("focus loss must not report an empty selection", reported.none { it.isEmpty() })

        onNodeWithText("hello world").performClick()
        waitForIdle()
        assertTrue("a tap inside the block dismisses the selection", reported.last().isEmpty())
    }

    @Test
    fun `dragging across text reports a selection`() = runComposeUiTest {
        val reported = mutableListOf<IntRange>()
        setContent {
            SelectableMarkdownBlock(
                text = AnnotatedString("hello world"),
                style = TextStyle(fontSize = 32.sp),
                clearSelection = false,
                onSelectionChange = { reported += it },
                onLinkClick = {},
            )
        }

        onNodeWithText("hello world").performMouseInput {
            dragAndDrop(
                start = Offset(2f, centerY),
                end = Offset(centerX, centerY),
                durationMillis = 400,
            )
        }
        waitForIdle()

        assertTrue("drag must report a non-empty range, got $reported", reported.any { !it.isEmpty() })
    }
}
