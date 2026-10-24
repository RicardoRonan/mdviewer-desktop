package dev.ricardo.mdviewer.feature.reader

import dev.ricardo.mdviewer.data.io.AnnotationTransfer
import dev.ricardo.mdviewer.data.io.ExportBundle
import dev.ricardo.mdviewer.data.io.ImportOutcome
import dev.ricardo.mdviewer.data.prefs.SettingsRepository
import dev.ricardo.mdviewer.data.repo.AnnotationRepository
import dev.ricardo.mdviewer.data.repo.DocumentRepository
import dev.ricardo.mdviewer.domain.anchor.AnchorEngine
import dev.ricardo.mdviewer.domain.markdown.MdBlock
import dev.ricardo.mdviewer.domain.markdown.MarkdownParser
import dev.ricardo.mdviewer.domain.markdown.isAnnotatable
import dev.ricardo.mdviewer.domain.markdown.plainText
import dev.ricardo.mdviewer.domain.model.Annotation
import dev.ricardo.mdviewer.domain.model.AnnotationKind
import dev.ricardo.mdviewer.domain.model.UserSettings
import dev.ricardo.mdviewer.ui.theme.HighlightColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ResolvedAnnotation(
    val annotation: Annotation,
    val blockId: String,
    val range: IntRange,
    val orphaned: Boolean,
)

data class ReaderUiState(
    val loading: Boolean = true,
    val documentName: String = "",
    val documentUri: String = "",
    val blocks: List<MdBlock> = emptyList(),
    val resolved: List<ResolvedAnnotation> = emptyList(),
    val initialScroll: Int = 0,
    val source: String = "",
    val editing: Boolean = false,
    val dirty: Boolean = false,
    val settings: UserSettings = UserSettings(),
    val error: String? = null,
    val message: String? = null,
)

class ReaderViewModel(
    documentPath: String,
    startInEditor: Boolean,
    private val documentRepository: DocumentRepository,
    private val annotationRepository: AnnotationRepository,
    private val settingsRepository: SettingsRepository,
    private val transfer: AnnotationTransfer,
    private val scope: CoroutineScope,
) {

    private var docUri: String = documentPath
    private val startInEditor = startInEditor
    private var savedSource: String = ""
    private var annotationWatch: Job? = null

    private val _state = MutableStateFlow(ReaderUiState())
    val state = _state.asStateFlow()

    init {
        load()
        scope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
    }

    fun load() {
        scope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val opened = documentRepository.open(docUri)
                docUri = opened.document.uri
                savedSource = opened.text
                val blocks = MarkdownParser.parse(opened.text)
                val annotations = annotationRepository.getForDocument(docUri)
                val resolved = resolveAll(annotations, blocks)
                _state.update {
                    it.copy(
                        loading = false,
                        documentName = opened.document.name,
                        documentUri = docUri,
                        blocks = blocks,
                        resolved = resolved,
                        initialScroll = opened.document.lastPosition,
                        source = opened.text,
                        editing = startInEditor,
                        dirty = false,
                    )
                }
                watchAnnotations(docUri)
            }.onFailure { error ->
                _state.update {
                    it.copy(loading = false, error = error.message ?: "Could not open document")
                }
            }
        }
    }

    fun setEditing(editing: Boolean) {
        _state.update { it.copy(editing = editing) }
    }

    fun updateDraft(text: String) {
        _state.update { it.copy(source = text, dirty = text != savedSource) }
    }

    suspend fun save() {
        val text = _state.value.source
        if (text == savedSource) {
            _state.update { it.copy(dirty = false) }
            return
        }
        documentRepository.saveText(docUri, text)
        savedSource = text
        val blocks = MarkdownParser.parse(text)
        val annotations = annotationRepository.getForDocument(docUri)
        val resolved = resolveAll(annotations, blocks)
        _state.update {
            it.copy(
                source = text,
                blocks = blocks,
                resolved = resolved,
                dirty = false,
                message = "Saved",
            )
        }
    }

    fun addHighlight(blockId: String, range: IntRange, color: String) {
        create(AnnotationKind.HIGHLIGHT, blockId, range, note = "", tags = emptyList(), color = color)
    }

    suspend fun rename(rawName: String) {
        val newPath = documentRepository.renameFile(docUri, rawName)
        docUri = newPath
        _state.update {
            it.copy(documentUri = newPath, documentName = File(newPath).name, message = "Renamed")
        }
        watchAnnotations(newPath)
    }

    suspend fun deleteDocument() {
        documentRepository.deleteFile(docUri)
    }

    fun addNote(blockId: String, range: IntRange, note: String, tags: List<String>) {
        create(AnnotationKind.NOTE, blockId, range, note = note, tags = tags)
    }

    fun delete(id: String) {
        scope.launch {
            annotationRepository.softDelete(id)
            refreshAnnotations()
            _state.update { it.copy(message = "Annotation deleted") }
        }
    }

    private fun create(
        kind: AnnotationKind,
        blockId: String,
        range: IntRange,
        note: String,
        tags: List<String>,
        color: String? = null,
    ) {
        scope.launch {
            val block = _state.value.blocks.firstOrNull { it.id == blockId } ?: return@launch
            val text = block.plainText()
            if (range.isEmpty() || range.last >= text.length) return@launch
            val settings = settingsRepository.settings.first()
            val chosen = color ?: when (kind) {
                AnnotationKind.NOTE -> if (settings.accentHighlight) ACCENT else "#EFEFEF"
                else -> HighlightColors.first().hex
            }
            val now = System.currentTimeMillis()
            val annotation = Annotation(
                id = UUID.randomUUID().toString(),
                docUri = docUri,
                kind = kind,
                color = chosen,
                selector = AnchorEngine.buildSelector(blockId, text, range.first, range.last + 1),
                note = note,
                tags = tags,
                createdAt = now,
                updatedAt = now,
            )
            annotationRepository.save(annotation)
            refreshAnnotations()
            _state.update {
                it.copy(message = if (kind == AnnotationKind.NOTE) "Note saved" else "Highlighted")
            }
        }
    }

    private fun watchAnnotations(uri: String) {
        annotationWatch?.cancel()
        annotationWatch = scope.launch {
            annotationRepository.observeForDocument(uri).collect { annotations ->
                val current = _state.value
                if (current.loading || current.documentUri != uri) return@collect
                _state.update { it.copy(resolved = resolveAll(annotations, it.blocks)) }
            }
        }
    }

    private suspend fun refreshAnnotations() {
        val blocks = _state.value.blocks
        val annotations = annotationRepository.getForDocument(docUri)
        _state.update { it.copy(resolved = resolveAll(annotations, blocks)) }
    }

    private suspend fun resolveAll(
        annotations: List<Annotation>,
        blocks: List<MdBlock>,
    ): List<ResolvedAnnotation> {
        val annotatable = blocks.filter { it.isAnnotatable() }
        return annotations.filter { !it.deleted }.map { annotation ->
            val resolved = resolveOne(annotation, annotatable)
            if (resolved.orphaned != annotation.orphaned) {
                annotationRepository.updateOrphaned(annotation, resolved.orphaned)
            }
            resolved
        }
    }

    private fun resolveOne(annotation: Annotation, blocks: List<MdBlock>): ResolvedAnnotation {
        val selector = annotation.selector
        blocks.firstOrNull { it.id == selector.blockId }?.let { block ->
            AnchorEngine.resolve(block.plainText(), selector)?.let { range ->
                return ResolvedAnnotation(annotation, block.id, range, orphaned = false)
            }
        }
        for (block in blocks) {
            AnchorEngine.resolve(block.plainText(), selector)?.let { range ->
                return ResolvedAnnotation(annotation, block.id, range, orphaned = false)
            }
        }
        return ResolvedAnnotation(
            annotation = annotation,
            blockId = selector.blockId,
            range = selector.start until selector.end,
            orphaned = true,
        )
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    fun savePosition(position: Int) {
        scope.launch { documentRepository.savePosition(docUri, position) }
    }

    suspend fun buildExport(): ExportBundle = transfer.buildExport(docUri)

    suspend fun importSidecar(text: String): ImportOutcome {
        val outcome = transfer.importSidecar(text, docUri)
        refreshAnnotations()
        return outcome
    }

    companion object {
        const val ACCENT = "#FFE066"
    }
}
