package dev.ricardo.mdviewer.data.repo

import dev.ricardo.mdviewer.data.AppPaths
import dev.ricardo.mdviewer.data.scan.MarkdownIndex
import dev.ricardo.mdviewer.data.store.JsonStore
import dev.ricardo.mdviewer.data.store.LibraryState
import dev.ricardo.mdviewer.domain.model.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class OpenedDocument(
    val document: Document,
    val text: String,
)

class DocumentRepository(
    private val store: JsonStore<LibraryState>,
    private val annotationRepository: AnnotationRepository,
    private val markdownIndex: MarkdownIndex,
) {

    fun observeRecents(): Flow<List<Document>> = store.state.map { state ->
        state.documents.sortedByDescending { it.lastOpened }
    }

    suspend fun find(uri: String): Document? =
        store.state.value.documents.firstOrNull { it.uri == uri }

    /** Reads a Markdown file from disk and records it as recently opened. */
    suspend fun open(path: String): OpenedDocument = withContext(Dispatchers.IO) {
        val file = canonicalFile(path)
        if (!file.isFile) throw IllegalStateException("This file no longer exists: $path")
        val bytes = runCatching { file.readBytes() }
            .getOrElse { throw IllegalStateException("Cannot read ${file.name}. Check its permissions.") }
        val text = bytes.toString(Charsets.UTF_8)
        val existing = store.state.value.documents.firstOrNull { it.uri == file.path }
        val document = Document(
            uri = file.path,
            name = file.name,
            size = bytes.size.toLong(),
            sha256 = sha256(bytes),
            pathHint = file.path,
            lastOpened = System.currentTimeMillis(),
            lastPosition = existing?.lastPosition ?: 0,
        )
        store.update { state ->
            state.copy(documents = state.documents.filterNot { it.uri == file.path } + document)
        }
        OpenedDocument(document, text)
    }

    suspend fun readText(path: String): String = withContext(Dispatchers.IO) {
        runCatching { canonicalFile(path).readText() }.getOrDefault("")
    }

    /** Writes the editor draft back to the file the reader opened. */
    suspend fun saveText(path: String, text: String) = withContext(Dispatchers.IO) {
        val file = canonicalFile(path)
        if (!file.isFile) throw IllegalStateException("This note is no longer a file on disk.")
        val bytes = text.toByteArray(Charsets.UTF_8)
        runCatching { file.writeBytes(bytes) }
            .getOrElse { throw IllegalStateException("Cannot write ${file.name}. Check its permissions.") }
        store.update { state ->
            state.copy(
                documents = state.documents.map { document ->
                    if (document.uri != file.path) {
                        document
                    } else {
                        document.copy(size = bytes.size.toLong(), sha256 = sha256(bytes), name = file.name)
                    }
                },
            )
        }
    }

    /**
     * Creates a Markdown file in [directory] (the library folder being browsed, or the default
     * MDViewer notes folder) and returns it opened so the editor can type straight into it.
     */
    suspend fun createNote(name: String, directory: File): OpenedDocument = withContext(Dispatchers.IO) {
        val dir = directory.canonicalFile
        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("Could not create ${dir.path}.")
        }
        val fileName = noteFileName(name)
        val base = fileName.removeSuffix(".md")
        var target = File(dir, fileName)
        var suffix = 2
        while (target.exists()) {
            target = File(dir, "$base-$suffix.md")
            suffix++
        }
        target.writeText("# ${target.nameWithoutExtension}\n\n")
        open(target.path)
    }

    /** Renames the file in its current folder and keeps annotations attached to the new path. */
    suspend fun renameFile(path: String, rawName: String): String = withContext(Dispatchers.IO) {
        val file = canonicalFile(path)
        if (!file.isFile) throw IllegalStateException("This note is no longer a file on disk.")
        val parent = file.parentFile ?: throw IllegalStateException("Cannot rename this file.")
        val target = File(parent, noteFileName(rawName))
        if (target.absolutePath == file.absolutePath) return@withContext file.absolutePath
        if (target.exists()) throw IllegalStateException("A file named ${target.name} already exists.")
        if (!file.renameTo(target)) throw IllegalStateException("Could not rename ${file.name}.")
        val oldPath = file.absolutePath
        val newPath = target.absolutePath
        store.update { state ->
            val existing = state.documents.firstOrNull { it.uri == oldPath }
            val others = state.documents.filterNot { it.uri == oldPath || it.uri == newPath }
            if (existing == null) {
                state
            } else {
                state.copy(
                    documents = others + existing.copy(
                        uri = newPath,
                        name = target.name,
                        pathHint = newPath,
                    ),
                )
            }
        }
        annotationRepository.reassign(oldPath, newPath)
        newPath
    }

    /** Deletes the file from disk and forgets its document record and annotations. */
    suspend fun deleteFile(path: String) = withContext(Dispatchers.IO) {
        val file = canonicalFile(path)
        if (file.isFile && !file.delete()) {
            throw IllegalStateException("Could not delete ${file.name}. Check its permissions.")
        }
        store.update { state ->
            state.copy(documents = state.documents.filterNot { it.uri == path })
        }
        annotationRepository.deleteForDocument(path)
    }

    suspend fun savePosition(uri: String, position: Int) = store.update { state ->
        state.copy(
            documents = state.documents.map { document ->
                if (document.uri == uri) {
                    document.copy(lastPosition = position, lastOpened = System.currentTimeMillis())
                } else {
                    document
                }
            },
        )
    }

    /** Refreshes size/hash/last-opened for a document the library rescanned. */
    suspend fun touch(path: String) = withContext(Dispatchers.IO) {
        val file = runCatching { canonicalFile(path) }.getOrNull() ?: return@withContext
        if (!file.isFile) return@withContext
        store.update { state ->
            state.copy(
                documents = state.documents.map { document ->
                    if (document.uri == path) {
                        document.copy(size = file.length(), name = file.name)
                    } else {
                        document
                    }
                },
            )
        }
    }

    /** Cross-device identity: match a sidecar to a local document by hash, then name+size. */
    suspend fun matchLocalDocument(sha256: String?, name: String, size: Long): Document? {
        val documents = store.state.value.documents
        if (sha256 != null) {
            documents.firstOrNull { it.sha256 == sha256 }?.let { return it }
        }
        return documents.firstOrNull { it.name == name && it.size == size }
    }

    private fun canonicalFile(path: String): File = File(path).canonicalFile

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

internal fun noteFileName(raw: String): String {
    val trimmed = raw.trim()
        .removeSuffix(".markdown")
        .removeSuffix(".md")
        .removeSuffix(".MD")
    val cleaned = trimmed
        .replace(Regex("""[\\/:*?"<>|]"""), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(80)
    return "${cleaned.ifBlank { "Untitled" }}.md"
}

/** Default folder for new notes when the library root is not configured. */
fun defaultNotesDirectory(): File = AppPaths.defaultNotesDir
