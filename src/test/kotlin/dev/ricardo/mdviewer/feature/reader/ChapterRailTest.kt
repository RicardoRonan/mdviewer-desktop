package dev.ricardo.mdviewer.feature.reader

import dev.ricardo.mdviewer.domain.markdown.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterRailTest {

    private fun chapters(count: Int): List<Chapter> = (1..count).map { Chapter("b$it", 2, "Heading $it") }

    @Test
    fun `outline keeps document order and drops blank headings`() {
        val blocks = MarkdownParser.parse("# First\n\n## Second\n\n##")
        val outline = chapterOutline(blocks)
        assertEquals(listOf("First", "Second"), outline.map { it.title })
        assertEquals(listOf(1, 2), outline.map { it.level })
    }

    @Test
    fun `short documents list every chapter`() {
        val list = chapters(3)
        assertEquals(list, visibleChapters(list, null))
    }

    @Test
    fun `long documents show at most five`() {
        val list = chapters(40)
        assertEquals(5, visibleChapters(list, null).size)
    }

    @Test
    fun `window follows the active chapter`() {
        val list = chapters(40)
        val window = visibleChapters(list, "b1")
        assertEquals(listOf("b1", "b2", "b3", "b4", "b5"), window.map { it.blockId })
    }

    @Test
    fun `window clamps at the end of the document`() {
        val list = chapters(40)
        val window = visibleChapters(list, "b40")
        assertEquals(listOf("b36", "b37", "b38", "b39", "b40"), window.map { it.blockId })
    }

    @Test
    fun `window slides forward as the reader scrolls`() {
        val list = chapters(40)
        val earlier = visibleChapters(list, "b10").map { it.blockId }
        val later = visibleChapters(list, "b30").map { it.blockId }
        assertTrue(earlier.contains("b10"))
        assertTrue(later.contains("b30"))
        assertEquals(5, later.size)
        assertEquals(earlier.last(), "b12")
        assertEquals(later.first(), "b28")
    }

    @Test
    fun `unknown active chapter falls back to the first rows`() {
        val list = chapters(40)
        assertEquals(listOf("b1", "b2", "b3", "b4", "b5"), visibleChapters(list, "missing").map { it.blockId })
    }
}
