package dev.ricardo.mdviewer.domain.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun `parses headings with levels`() {
        val blocks = MarkdownParser.parse("# Title\n\n## Subtitle")
        assertEquals(2, blocks.size)
        assertEquals(1, (blocks[0] as MdBlock.Heading).level)
        assertEquals("Title", blocks[0].plainText())
        assertEquals(2, (blocks[1] as MdBlock.Heading).level)
    }

    @Test
    fun `parses fenced code with language`() {
        val source = "```kotlin\nval x = 1\n```"
        val block = MarkdownParser.parse(source).single() as MdBlock.Code
        assertEquals("kotlin", block.language)
        assertEquals("val x = 1", block.code)
    }

    @Test
    fun `inline text offsets match rendered text`() {
        val blocks = MarkdownParser.parse("This is **bold** and `code` text")
        val paragraph = blocks.single() as MdBlock.Paragraph
        val text = paragraph.plainText()
        assertEquals("This is bold and code text", text)
        val boldStart = text.indexOf("bold")
        assertEquals(8, boldStart)
    }

    @Test
    fun `bare urls become links`() {
        val paragraph = MarkdownParser.parse("See https://example.com/a. next").single() as MdBlock.Paragraph
        val link = paragraph.inlines.filterIsInstance<MdInline.Link>().single()
        assertEquals("https://example.com/a", link.url)
        assertEquals("https://example.com/a", link.text)
        assertEquals("See https://example.com/a. next", paragraph.plainText())
    }

    @Test
    fun `parses links and lists`() {
        val blocks = MarkdownParser.parse("- [site](https://x.dev)\n- second")
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is MdBlock.ListItem)
        assertEquals("site", blocks[0].plainText())
        assertEquals("second", blocks[1].plainText())
    }

    @Test
    fun `parses gfm tables`() {
        val source = "| a | b |\n| --- | --- |\n| 1 | 2 |"
        val table = MarkdownParser.parse(source).single() as MdBlock.Table
        assertEquals(listOf("a", "b"), table.header)
        assertEquals(listOf("1", "2"), table.rows.single())
    }

    @Test
    fun `parses blockquotes and rules`() {
        val blocks = MarkdownParser.parse("> quoted words\n\n---")
        assertTrue(blocks[0] is MdBlock.Quote)
        assertEquals("quoted words", blocks[0].plainText())
        assertTrue(blocks[1] is MdBlock.Rule)
    }
}
