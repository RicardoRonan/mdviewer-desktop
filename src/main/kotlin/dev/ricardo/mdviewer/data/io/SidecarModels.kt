package dev.ricardo.mdviewer.data.io

import kotlinx.serialization.Serializable

const val SIDECAR_SCHEMA = "mdviewer.annotations/1"

@Serializable
data class SidecarFile(
    val schema: String = SIDECAR_SCHEMA,
    val generator: String = "MDViewer 1.0",
    val exportedAt: Long = 0L,
    val document: SidecarDocument,
    val annotations: List<SidecarAnnotation>,
)

@Serializable
data class SidecarDocument(
    val name: String,
    val size: Long,
    val sha256: String? = null,
    val pathHint: String? = null,
)

@Serializable
data class SidecarAnnotation(
    val id: String,
    val kind: String,
    val color: String,
    val exact: String,
    val prefix: String = "",
    val suffix: String = "",
    val start: Int,
    val end: Int,
    val blockId: String,
    val note: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
)
