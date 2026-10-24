package dev.ricardo.mdviewer.data.repo

import dev.ricardo.mdviewer.data.store.JsonStore
import dev.ricardo.mdviewer.data.store.LibraryState
import dev.ricardo.mdviewer.domain.model.Annotation
import dev.ricardo.mdviewer.domain.model.AnnotationKind
import dev.ricardo.mdviewer.domain.model.QuoteSelector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AnnotationRepositoryTest {

    private fun newStore(dir: File) = JsonStore(File(dir, "library.json"), LibraryState.serializer()) { LibraryState() }

    private fun annotation(
        id: String,
        docUri: String = "/tmp/note.md",
        updatedAt: Long = 1L,
        deleted: Boolean = false,
        start: Int = 0,
    ) = Annotation(
        id = id,
        docUri = docUri,
        kind = AnnotationKind.HIGHLIGHT,
        color = "#FFE8A3",
        selector = QuoteSelector("quote", "", "", start, start + 5, "b0"),
        note = "",
        tags = emptyList(),
        createdAt = updatedAt,
        updatedAt = updatedAt,
        deleted = deleted,
    )

    @Test
    fun `save replaces by id and observes only live annotations`() = runTest {
        val dir = File.createTempFile("annotations", "").apply { delete(); mkdirs() }
        try {
            val repo = AnnotationRepository(newStore(dir))
            repo.save(annotation("a", start = 20))
            repo.save(annotation("b", start = 2))
            repo.save(annotation("a", start = 20).copy(note = "edited"))

            val live = repo.observeForDocument("/tmp/note.md").first()
            assertEquals(listOf("b", "a"), live.map { it.id })
            assertEquals("edited", live.last().note)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `soft delete hides the annotation but keeps the tombstone`() = runTest {
        val dir = File.createTempFile("annotations", "").apply { delete(); mkdirs() }
        try {
            val repo = AnnotationRepository(newStore(dir))
            repo.save(annotation("a"))
            repo.softDelete("a", timestamp = 99L)

            assertTrue(repo.observeAll().first().isEmpty())
            val all = repo.getForDocument("/tmp/note.md").single()
            assertTrue(all.deleted)
            assertEquals(99L, all.updatedAt)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `merge keeps the newest version and never resurrects tombstones`() = runTest {
        val dir = File.createTempFile("annotations", "").apply { delete(); mkdirs() }
        try {
            val repo = AnnotationRepository(newStore(dir))
            repo.save(annotation("old", updatedAt = 10).copy(note = "local"))
            repo.save(annotation("deleted", updatedAt = 50, deleted = true))

            val result = repo.merge(
                listOf(
                    annotation("new", updatedAt = 5),
                    annotation("old", updatedAt = 20).copy(note = "remote"),
                    annotation("stale", updatedAt = 4),
                    annotation("deleted", updatedAt = 40).copy(deleted = false),
                ),
            )

            val merged = repo.getForDocument("/tmp/note.md").associateBy { it.id }
            assertEquals("remote", merged.getValue("old").note)
            assertEquals(2, result.added)
            assertEquals(1, result.updated)
            assertEquals(1, result.skipped)
            assertTrue(merged.getValue("deleted").deleted)
            assertTrue(merged.containsKey("new"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `reassign moves annotations to the renamed path`() = runTest {
        val dir = File.createTempFile("annotations", "").apply { delete(); mkdirs() }
        try {
            val repo = AnnotationRepository(newStore(dir))
            repo.save(annotation("a", docUri = "/tmp/old.md"))
            repo.reassign("/tmp/old.md", "/tmp/new.md")

            assertTrue(repo.observeForDocument("/tmp/old.md").first().isEmpty())
            assertEquals("a", repo.observeForDocument("/tmp/new.md").first().single().id)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `deleteForDocument drops every annotation of that file`() = runTest {
        val dir = File.createTempFile("annotations", "").apply { delete(); mkdirs() }
        try {
            val repo = AnnotationRepository(newStore(dir))
            repo.save(annotation("a", docUri = "/tmp/a.md"))
            repo.save(annotation("b", docUri = "/tmp/b.md"))
            repo.deleteForDocument("/tmp/a.md")
            assertTrue(repo.getForDocument("/tmp/a.md").isEmpty())
            assertEquals(1, repo.getForDocument("/tmp/b.md").size)
        } finally {
            dir.deleteRecursively()
        }
    }
}
