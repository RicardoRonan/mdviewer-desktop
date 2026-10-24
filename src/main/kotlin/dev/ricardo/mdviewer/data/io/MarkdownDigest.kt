package dev.ricardo.mdviewer.data.io

import dev.ricardo.mdviewer.domain.model.Annotation
import dev.ricardo.mdviewer.domain.model.AnnotationKind
import dev.ricardo.mdviewer.domain.model.Document

/** Builds a human-readable companion digest so annotations are useful outside the app. */
object MarkdownDigest {

    fun build(document: Document, annotations: List<Annotation>): String {
        val visible = annotations.filter { !it.deleted }
        val builder = StringBuilder()
        builder.append("# Annotations: ").append(document.name).append("\n\n")
        builder.append("_").append(visible.size).append(" annotation(s). Exported by MDViewer._\n\n")
        if (visible.isEmpty()) {
            builder.append("No annotations yet.\n")
            return builder.toString()
        }
        val grouped = visible.groupBy { it.kind }
        for (kind in AnnotationKind.entries) {
            val items = grouped[kind] ?: continue
            builder.append("## ").append(kind.prettyName()).append("\n\n")
            items.sortedBy { it.selector.start }.forEach { annotation ->
                builder.append("> ").append(annotation.selector.exact.replace("\n", " ")).append("\n\n")
                if (annotation.note.isNotBlank()) {
                    builder.append(annotation.note).append("\n\n")
                }
                if (annotation.tags.isNotEmpty()) {
                    builder.append("Tags: ").append(annotation.tags.joinToString(", ") { "#$it" }).append("\n\n")
                }
            }
        }
        return builder.toString()
    }

    private fun AnnotationKind.prettyName(): String = when (this) {
        AnnotationKind.HIGHLIGHT -> "Highlights"
        AnnotationKind.NOTE -> "Notes"
        AnnotationKind.BOOKMARK -> "Bookmarks"
    }
}
