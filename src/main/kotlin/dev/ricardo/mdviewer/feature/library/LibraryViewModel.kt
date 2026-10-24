package dev.ricardo.mdviewer.feature.library

import dev.ricardo.mdviewer.data.prefs.SettingsRepository
import dev.ricardo.mdviewer.data.repo.DocumentRepository
import dev.ricardo.mdviewer.data.repo.defaultNotesDirectory
import dev.ricardo.mdviewer.data.scan.IndexedMarkdown
import dev.ricardo.mdviewer.data.scan.MarkdownIndex
import dev.ricardo.mdviewer.domain.model.Document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class LibraryState(
    val checked: Boolean = false,
    val hasRoot: Boolean = false,
    val rootPath: String = "",
    val scanning: Boolean = false,
    val files: List<IndexedMarkdown> = emptyList(),
)

class LibraryViewModel(
    private val settingsRepository: SettingsRepository,
    private val documentRepository: DocumentRepository,
    private val markdownIndex: MarkdownIndex,
    private val scope: CoroutineScope,
) {

    val recents: StateFlow<List<Document>> = documentRepository.observeRecents()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _library = MutableStateFlow(LibraryState())
    val library: StateFlow<LibraryState> = _library.asStateFlow()

    private val _folderPath = MutableStateFlow("")
    val folderPath: StateFlow<String> = _folderPath.asStateFlow()

    private var scanJob: Job? = null

    init {
        refresh()
    }

    suspend fun createNote(name: String, folderPath: String): String = withContext(Dispatchers.IO) {
        val rootPath = settingsRepository.current().libraryRoot
        val directory = if (rootPath.isBlank()) {
            defaultNotesDirectory()
        } else {
            File(rootPath, folderPath)
        }
        documentRepository.createNote(name, directory).document.uri
    }

    suspend fun renameFile(path: String, name: String): String =
        withContext(Dispatchers.IO) { documentRepository.renameFile(path, name) }

    suspend fun deleteFile(path: String) =
        withContext(Dispatchers.IO) { documentRepository.deleteFile(path) }

    fun openFolder(path: String) {
        _folderPath.value = path.trim('/')
    }

    fun goUp() {
        _folderPath.update { current ->
            if (current.isEmpty()) "" else current.substringBeforeLast('/', "")
        }
    }

    fun setRoot(directory: File) {
        _folderPath.value = ""
        scanJob?.cancel()
        scanJob = scope.launch {
            settingsRepository.setLibraryRoot(directory.absolutePath)
            refreshNow()
        }
    }

    fun clearRoot() {
        _folderPath.value = ""
        scanJob?.cancel()
        scanJob = scope.launch {
            settingsRepository.setLibraryRoot("")
            refreshNow()
        }
    }

    fun refresh() {
        scanJob?.cancel()
        scanJob = scope.launch { refreshNow() }
    }

    private suspend fun refreshNow() {
        val rootPath = settingsRepository.current().libraryRoot
        val root = rootPath.takeIf { it.isNotBlank() }?.let(::File)
        val hasRoot = root?.isDirectory == true
        _library.update {
            it.copy(
                checked = true,
                hasRoot = hasRoot,
                rootPath = if (hasRoot) rootPath else "",
                scanning = hasRoot,
                files = if (hasRoot) it.files else emptyList(),
            )
        }
        val files = if (hasRoot) {
            withContext(Dispatchers.IO) {
                runCatching { markdownIndex.scan(root) }.getOrDefault(emptyList())
            }
        } else {
            emptyList()
        }
        _library.update { it.copy(scanning = false, files = files) }
    }
}
