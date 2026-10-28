package dev.ricardo.mdviewer.feature.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ChapterRailUiTest {

    private fun chapters(count: Int = 8) = (1..count).map { Chapter("b$it", 2, "Heading $it") }

    @Test
    fun `hovering the rail opens the panel and picking a heading jumps`() = runComposeUiTest {
        val jumps = mutableListOf<String>()
        setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
                ChapterRail(
                    chapters = chapters(),
                    activeId = "b1",
                    onJump = { jumps += it },
                    scrollState = rememberScrollState(),
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }

        onNodeWithContentDescription("Chapters").performMouseInput { moveTo(Offset(4f, 4f)) }
        waitForIdle()
        onNodeWithText("Heading 3").assertIsDisplayed()

        onNodeWithContentDescription("Go to Heading 3").performClick()
        waitForIdle()
        assertEquals(listOf("b3"), jumps)
    }

    @Test
    fun `focusing the rail opens it, escape closes it`() = runComposeUiTest {
        setContent {
            Box(Modifier.fillMaxSize()) {
                ChapterRail(
                    chapters = chapters(),
                    activeId = "b1",
                    onJump = {},
                    scrollState = rememberScrollState(),
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }

        onNodeWithContentDescription("Chapters").performSemanticsAction(SemanticsActions.RequestFocus)
        waitForIdle()
        onNodeWithText("Heading 1").assertIsDisplayed()

        onNodeWithContentDescription("Chapters").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        assertTrue(
            "escape must close the panel",
            onAllNodesWithText("Heading 1").fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun `arrow keys move the jump target down the outline`() = runComposeUiTest {
        val jumps = mutableListOf<String>()
        setContent {
            Box(Modifier.fillMaxSize()) {
                ChapterRail(
                    chapters = chapters(),
                    activeId = "b1",
                    onJump = { jumps += it },
                    scrollState = rememberScrollState(),
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }

        onNodeWithContentDescription("Chapters").performSemanticsAction(SemanticsActions.RequestFocus)
        waitForIdle()

        onNodeWithContentDescription("Chapters").performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionDown)
        }
        waitForIdle()

        assertEquals(listOf("b2", "b3"), jumps)
    }
}
