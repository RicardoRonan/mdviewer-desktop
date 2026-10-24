package dev.ricardo.mdviewer.data.io

import dev.ricardo.mdviewer.domain.model.Annotation
import dev.ricardo.mdviewer.domain.model.AnnotationKind
import dev.ricardo.mdviewer.domain.model.Document
import dev.ricardo.mdviewer.domain.model.QuoteSelector
import kotlinx.serialization.json.Json

object SidecarCodec {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(file: SidecarFile): String = json.encodeToString(SidecarFile.serializer(), file)

    fun decode(text: String): SidecarFile = json.decodeFromString(SidecarFile.serializer(), text)
}

fun buildSidecar(
    document: Document,
    annotations: List<Annotation>,
    exportedAt: Long,
): SidecarFile = SidecarFile(
    exportedAt = exportedAt,
    document = SidecarDocument(
        name = document.name,
        size = document.size,
        sha256 = document.sha256,
        pathHint = document.pathHint,
    ),
    annotations = annotations.map { it.toSidecar() },
)

fun Annotation.toSidecar(): SidecarAnnotation = SidecarAnnotation(
    id = id,
    kind = kind.name,
    color = color,
    exact = selector.exact,
    prefix = selector.prefix,
    suffix = selector.suffix,
    start = selector.start,
    end = selector.end,
    blockId = selector.blockId,
    note = note,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deleted = deleted,
)

fun SidecarAnnotation.toDomain(docUri: String): Annotation = Annotation(
    id = id,
    docUri = docUri,
    kind = runCatching { AnnotationKind.valueOf(kind) }.getOrDefault(AnnotationKind.HIGHLIGHT),
    color = color,
    selector = QuoteSelector(
        exact = exact,
        prefix = prefix,
        suffix = suffix,
        start = start,
        end = end,
        blockId = blockId,
    ),
    note = note,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deleted = deleted,
    orphaned = false,
)
