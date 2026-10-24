package dev.ricardo.mdviewer.data.store

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class JsonStoreTest {

    private fun tempDir(): File = File.createTempFile("jsonstore", "").apply {
        delete()
        mkdirs()
    }

    @Test
    fun `starts from defaults and persists updates`() = runTest {
        val dir = tempDir()
        try {
            val file = File(dir, "state.json")
            val store = JsonStore(file, LibraryState.serializer()) { LibraryState() }
            assertTrue(store.state.value.documents.isEmpty())

            store.update { it.copy(documents = listOf(document("a.md"))) }

            assertEquals(1, store.state.value.documents.size)
            assertTrue(file.isFile)

            val reloaded = JsonStore(file, LibraryState.serializer()) { LibraryState() }
            assertEquals("a.md", reloaded.state.value.documents.single().name)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `falls back to defaults when the file is corrupt`() = runTest {
        val dir = tempDir()
        try {
            val file = File(dir, "state.json")
            file.writeText("{ not json")
            val store = JsonStore(file, LibraryState.serializer()) { LibraryState() }
            assertTrue(store.state.value.annotations.isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `creates parent directories on first write`() = runTest {
        val dir = tempDir()
        try {
            val file = File(dir, "nested/deeper/state.json")
            val store = JsonStore(file, LibraryState.serializer()) { LibraryState() }
            store.update { it.copy(documents = listOf(document("b.md"))) }
            assertTrue(file.isFile)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun document(name: String) = dev.ricardo.mdviewer.domain.model.Document(
        uri = "/tmp/$name",
        name = name,
        size = 10,
        sha256 = null,
        pathHint = null,
        lastOpened = 1L,
        lastPosition = 0,
    )
}
