package dev.ricardo.mdviewer.data.io

import dev.ricardo.mdviewer.data.repo.AnnotationRepository
import dev.ricardo.mdviewer.data.repo.DocumentRepository
import dev.ricardo.mdviewer.data.repo.MergeResult
import dev.ricardo.mdviewer.domain.model.Document

data class ExportBundle(
    val json: String,
    val digest: String,
    val suggestedJsonName: String,
    val suggestedDigestName: String,
)

data class ImportOutcome(
    val matchedDocument: Document?,
    val incomingCount: Int,
    val merge: MergeResult?,
    val schema: String,
)

class AnnotationTransfer(
    private val annotationRepository: AnnotationRepository,
    private val documentRepository: DocumentRepository,
) {
    suspend fun buildExport(docUri: String): ExportBundle {
        val document = documentRepository.find(docUri)
        val annotations = annotationRepository.getForDocument(docUri)
        val baseName = document?.name?.substringBeforeLast('.') ?: "document"
        val exportedAt = System.currentTimeMillis()
        val safeDoc = document ?: Document(
            uri = docUri,
            name = baseName,
            size = 0,
            sha256 = null,
            pathHint = null,
            lastOpened = exportedAt,
            lastPosition = 0,
        )
        val sidecar = buildSidecar(safeDoc, annotations, exportedAt)
        return ExportBundle(
            json = SidecarCodec.encode(sidecar),
            digest = MarkdownDigest.build(safeDoc, annotations),
            suggestedJsonName = "$baseName.md.annot.json",
            suggestedDigestName = "$baseName.md.annotations.md",
        )
    }

    suspend fun importSidecar(text: String, targetUri: String): ImportOutcome {
        val sidecar = SidecarCodec.decode(text)
        val incoming = sidecar.annotations.map { it.toDomain(targetUri) }
        val localDocument = documentRepository.matchLocalDocument(
            sha256 = sidecar.document.sha256,
            name = sidecar.document.name,
            size = sidecar.document.size,
        )
        val merge = annotationRepository.merge(incoming)
        return ImportOutcome(
            matchedDocument = localDocument,
            incomingCount = incoming.size,
            merge = merge,
            schema = sidecar.schema,
        )
    }
}
