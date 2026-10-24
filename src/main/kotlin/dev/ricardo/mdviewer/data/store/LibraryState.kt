package dev.ricardo.mdviewer.data.store

import dev.ricardo.mdviewer.domain.model.Annotation
import dev.ricardo.mdviewer.domain.model.Document
import kotlinx.serialization.Serializable

/** Everything the desktop app persists: the document index plus every annotation version. */
@Serializable
data class LibraryState(
    val documents: List<Document> = emptyList(),
    val annotations: List<Annotation> = emptyList(),
)
