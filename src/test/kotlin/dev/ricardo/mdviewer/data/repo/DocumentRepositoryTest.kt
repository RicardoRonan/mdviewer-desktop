package dev.ricardo.mdviewer.data.repo

import dev.ricardo.mdviewer.data.scan.MarkdownIndex
import dev.ricardo.mdviewer.data.store.JsonStore
import dev.ricardo.mdviewer.data.store.LibraryState
import dev.ricardo.mdviewer.domain.model.Annotation
import dev.ricardo.mdviewer.domain.model.AnnotationKind
import dev.ricardo.mdviewer.domain.model.QuoteSelector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DocumentRepositoryTest {

    private class Fixture {
        val dir: File = File.createTempFile("docrepo", "").apply { delete(); mkdirs() }
        val store = JsonStore(File(dir, "library.json"), LibraryState.serializer()) { LibraryState() }
        val annotations = AnnotationRepository(store)
        val documents = DocumentRepository(store, annotations, MarkdownIndex())
        val notes: File = File(dir, "notes").apply { mkdirs() }

        fun clean() = dir.deleteRecursively()
    }

    private fun annotation(id: String, docUri: String) = Annotation(
        id = id,
        docUri = docUri,
        kind = AnnotationKind.NOTE,
        color = "#FFE066",
        selector = QuoteSelector("quote", "", "", 0, 5, "b0"),
        note = "note",
        tags = listOf("tag"),
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `open records the document and keeps the last reading position`() = runTest {
        val fixture = Fixture()
        try {
            val file = File(fixture.notes, "note.md").apply { writeText("# Title\n\nBody") }
            val first = fixture.documents.open(file.path)

            assertEquals("note.md", first.document.name)
            assertEquals("# Title\n\nBody", first.text)
            assertNotNull(first.document.sha256)
            assertEquals(1, fixture.documents.observeRecents().first().size)

            fixture.documents.savePosition(first.document.uri, 480)
            val reopened = fixture.documents.open(file.path)
            assertEquals(480, reopened.document.lastPosition)
            assertEquals(1, fixture.documents.observeRecents().first().size)
        } finally {
            fixture.clean()
        }
    }

    @Test
    fun `open rejects a missing file`() = runTest {
        val fixture = Fixture()
        try {
            val error = runCatching { fixture.documents.open(File(fixture.notes, "gone.md").path) }
            assertTrue(error.isFailure)
            assertTrue(error.exceptionOrNull()?.message.orEmpty().contains("no longer exists"))
        } finally {
            fixture.clean()
        }
    }

    @Test
    fun `rename moves the file and carries annotations to the new path`() = runTest {
        val fixture = Fixture()
        try {
            val file = File(fixture.notes, "draft.md").apply { writeText("hello") }
            val opened = fixture.documents.open(file.path)
            fixture.annotations.save(annotation("a1", opened.document.uri))

            val renamedPath = fixture.documents.renameFile(opened.document.uri, "final.md")

            // Compare canonical paths: on some Windows runners java.io.tmpdir resolves via an
            // 8.3 short name (e.g. RUNNER~1), while production always canonicalizes URIs.
            assertEquals(File(fixture.notes, "final.md").canonicalPath, File(renamedPath).canonicalPath)
            assertFalse(file.exists())
            assertEquals("a1", fixture.annotations.observeForDocument(renamedPath).first().single().id)
            assertEquals("final.md", fixture.documents.observeRecents().first().single().name)
        } finally {
            fixture.clean()
        }
    }

    @Test
    fun `delete removes the file, the document record and its annotations`() = runTest {
        val fixture = Fixture()
        try {
            val file = File(fixture.notes, "gone.md").apply { writeText("bye") }
            val opened = fixture.documents.open(file.path)
            fixture.annotations.save(annotation("a1", opened.document.uri))

            fixture.documents.deleteFile(opened.document.uri)

            assertFalse(file.exists())
            assertTrue(fixture.documents.observeRecents().first().isEmpty())
            assertTrue(fixture.annotations.observeForDocument(opened.document.uri).first().isEmpty())
        } finally {
            fixture.clean()
        }
    }

    @Test
    fun `saveText rewrites the file and refreshes size and hash`() = runTest {
        val fixture = Fixture()
        try {
            val file = File(fixture.notes, "edit.md").apply { writeText("short") }
            val opened = fixture.documents.open(file.path)
            val before = opened.document.sha256

            fixture.documents.saveText(opened.document.uri, "a much longer body")

            assertEquals("a much longer body", file.readText())
            val updated = fixture.documents.observeRecents().first().single()
            assertEquals(file.length(), updated.size)
            assertTrue(before != updated.sha256)
        } finally {
            fixture.clean()
        }
    }

    @Test
    fun `createNote writes into the chosen folder and avoids collisions`() = runTest {
        val fixture = Fixture()
        try {
            val first = fixture.documents.createNote("Daily note", fixture.notes)
            val second = fixture.documents.createNote("Daily note", fixture.notes)

            assertEquals("Daily note.md", first.document.name)
            assertEquals("Daily note-2.md", second.document.name)
            assertTrue(File(fixture.notes, "Daily note.md").readText().startsWith("# Daily note"))
        } finally {
            fixture.clean()
        }
    }

    @Test
    fun `matchLocalDocument prefers the content hash over name and size`() = runTest {
        val fixture = Fixture()
        try {
            val one = File(fixture.notes, "one.md").apply { writeText("same-size") }
            val two = File(fixture.notes, "two.md").apply { writeText("different body") }
            fixture.documents.open(one.path)
            val openedTwo = fixture.documents.open(two.path)

            val byHash = fixture.documents.matchLocalDocument(openedTwo.document.sha256, "one.md", 1)
            assertEquals(openedTwo.document.uri, byHash?.uri)

            val byName = fixture.documents.matchLocalDocument(null, "one.md", one.length())
            assertEquals("one.md", byName?.name)

            assertNull(fixture.documents.matchLocalDocument(null, "unknown.md", 123))
        } finally {
            fixture.clean()
        }
    }
}
