package dev.ricardo.mdviewer.domain.model

import kotlinx.serialization.Serializable

/**
 * Portable document identity. The path changes when files move, so cross-device matching relies
 * on [sha256] first, then [name] + [size], with [pathHint] for disambiguation.
 */
@Serializable
data class Document(
    val uri: String,
    val name: String,
    val size: Long,
    val sha256: String?,
    val pathHint: String?,
    val lastOpened: Long,
    val lastPosition: Int,
)
