package dev.ricardo.mdviewer.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `relative time buckets`() {
        val now = 1_700_000_000_000L
        assertEquals("just now", relativeTime(now - 10_000, now))
        assertEquals("5m ago", relativeTime(now - 5 * 60_000, now))
        assertEquals("3h ago", relativeTime(now - 3 * 3_600_000, now))
        assertEquals("2d ago", relativeTime(now - 2 * 86_400_000, now))
        assertEquals("never", relativeTime(0, now))
    }

    @Test
    fun `human sizes`() {
        assertEquals("512 B", humanSize(512))
        assertEquals("2 KB", humanSize(2048))
        assertEquals("1.5 MB", humanSize((1.5 * 1024 * 1024).toLong()))
    }
}
