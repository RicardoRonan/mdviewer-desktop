package dev.ricardo.mdviewer.data.scan

import dev.ricardo.mdviewer.data.repo.noteFileName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MarkdownIndexTest {

    @Test
    fun collectFindsNestedMarkdownAndSkipsNoise() {
        val root = File.createTempFile("mdindex", "").apply {
            delete()
            mkdirs()
        }
        try {
            File(root, "notes").mkdirs()
            File(root, "notes/daily.md").writeText("# today")
            File(root, "notes/draft.markdown").writeText("draft")
            File(root, "notes/photo.jpg").writeText("nope")
            File(root, "node_modules").mkdirs()
            File(root, "node_modules/readme.md").writeText("skip")
            File(root, ".hidden").mkdirs()
            File(root, ".hidden/secret.md").writeText("skip")

            val found = collect(listOf(root)).map { it.name }

            assertEquals(listOf("daily.md", "draft.markdown"), found.sorted())
            val page = folderPage(collect(listOf(root)), "")
            assertEquals(listOf("notes"), page.folders.map { it.name })
            assertEquals(2, page.folders.single().markdownCount)
            val inside = folderPage(collect(listOf(root)), "notes")
            assertEquals(listOf("daily.md", "draft.markdown"), inside.files.map { it.name })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun folderPageSplitsNestedPaths() {
        val files = listOf(
            IndexedMarkdown("/a/readme.md", "readme.md", "", 1, 0),
            IndexedMarkdown("/a/book/outline.md", "outline.md", "book", 1, 0),
            IndexedMarkdown("/a/book/craft/voice.md", "voice.md", "book/craft", 1, 0),
        )
        val root = folderPage(files, "")
        assertEquals(listOf("book"), root.folders.map { it.name })
        assertEquals(2, root.folders.single().markdownCount)
        assertEquals(listOf("readme.md"), root.files.map { it.name })
        val book = folderPage(files, "book")
        assertEquals(listOf("craft"), book.folders.map { it.name })
        assertEquals(listOf("book/craft"), book.folders.map { it.path })
        assertEquals(listOf("outline.md"), book.files.map { it.name })
    }

    @Test
    fun noteFileNameCleansInput() {
        assertEquals("Untitled.md", noteFileName("  "))
        assertEquals("Daily note.md", noteFileName("Daily note.md"))
        assertEquals("a b.md", noteFileName("a/b"))
    }

    @Test
    fun isMarkdownNameAcceptsCommonExtensions() {
        assertTrue(isMarkdownName("Note.md"))
        assertTrue(isMarkdownName("a.markdown"))
        assertTrue(!isMarkdownName("archive.md.zip"))
        assertTrue(!isMarkdownName(".md"))
    }

    @Test
    fun scanIgnoresMissingRoot() {
        assertTrue(MarkdownIndex().scan(null).isEmpty())
        assertTrue(MarkdownIndex().scan(File("/definitely/not/here")).isEmpty())
    }
}
