package dev.ricardo.mdviewer.domain.anchor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AnchorEngineTest {

    @Test
    fun `builds a selector with surrounding context`() {
        val text = "The quick brown fox jumps over the lazy dog"
        val selector = AnchorEngine.buildSelector("b0", text, 4, 9)
        assertEquals("quick", selector.exact)
        assertEquals("b0", selector.blockId)
        assertEquals(4, selector.start)
        assertEquals(9, selector.end)
        assertEquals("The ", selector.prefix)
    }

    @Test
    fun `resolves after content is inserted before the quote`() {
        val original = "Alpha bravo charlie delta"
        val selector = AnchorEngine.buildSelector("b0", original, 6, 12) // "bravo "
        val edited = "Intro text. Alpha bravo charlie delta"
        val range = AnchorEngine.resolve(edited, selector)
        assertNotNull(range)
        assertEquals(selector.exact, edited.substring(range!!.first, range.last + 1))
    }

    @Test
    fun `resolves the correct occurrence using context`() {
        val text = "cat dog cat bird cat"
        val selector = AnchorEngine.buildSelector("b1", text, 12, 15) // third "cat"
        val range = AnchorEngine.resolve(text, selector)
        assertNotNull(range)
        assertEquals(12, range!!.first)
    }

    @Test
    fun `returns null when the quote no longer exists`() {
        val selector = AnchorEngine.buildSelector("b0", "hello world", 0, 5)
        assertNull(AnchorEngine.resolve("completely different text", selector))
    }

    @Test
    fun `survives whitespace-only edits`() {
        val selector = AnchorEngine.buildSelector("b0", "one two three", 4, 7)
        val range = AnchorEngine.resolve("one two three four", selector)
        assertNotNull(range)
    }
}
