package dev.ricardo.mdviewer.feature.notebook

import dev.ricardo.mdviewer.data.repo.AnnotationRepository
import dev.ricardo.mdviewer.data.repo.DocumentRepository
import dev.ricardo.mdviewer.domain.model.Annotation
import dev.ricardo.mdviewer.domain.model.AnnotationKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotebookItem(
    val annotation: Annotation,
    val docName: String,
)

class NotebookViewModel(
    private val annotationRepository: AnnotationRepository,
    documentRepository: DocumentRepository,
    private val scope: CoroutineScope,
) {

    private val filter = MutableStateFlow<AnnotationKind?>(null)

    val items: StateFlow<List<NotebookItem>> = combine(
        annotationRepository.observeAll(),
        documentRepository.observeRecents(),
        filter,
    ) { annotations, documents, kind ->
        val names = documents.associate { it.uri to it.name }
        annotations
            .filter { kind == null || it.kind == kind }
            .map { NotebookItem(it, names[it.docUri] ?: "Unknown document") }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeFilter: StateFlow<AnnotationKind?> = filter

    fun setFilter(kind: AnnotationKind?) {
        filter.value = kind
    }

    fun delete(id: String) {
        scope.launch { annotationRepository.softDelete(id) }
    }
}
