package dev.ricardo.mdviewer.domain.model

import kotlinx.serialization.Serializable

enum class AnnotationKind { HIGHLIGHT, NOTE, BOOKMARK }

/** W3C Web Annotation style quote selector, scoped to a single rendered block. */
@Serializable
data class QuoteSelector(
    val exact: String,
    val prefix: String,
    val suffix: String,
    val start: Int,
    val end: Int,
    val blockId: String,
)

@Serializable
data class Annotation(
    val id: String,
    val docUri: String,
    val kind: AnnotationKind,
    val color: String,
    val selector: QuoteSelector,
    val note: String,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val orphaned: Boolean = false,
) {
    val exact: String get() = selector.exact
    val range: IntRange get() = selector.start until selector.end
}

enum class AppTheme { SYSTEM, LIGHT, DARK }

@Serializable
data class UserSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val fontScale: Float = 1f,
    val accentHighlight: Boolean = true,
    val loadRemoteImages: Boolean = false,
    val maxLineWidthDp: Int = 700,
    /** Absolute path of the folder browsed by the library. Blank means no folder chosen yet. */
    val libraryRoot: String = "",
)
