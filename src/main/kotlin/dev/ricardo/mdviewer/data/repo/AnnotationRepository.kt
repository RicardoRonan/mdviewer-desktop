package dev.ricardo.mdviewer.data.repo

import dev.ricardo.mdviewer.data.store.JsonStore
import dev.ricardo.mdviewer.data.store.LibraryState
import dev.ricardo.mdviewer.domain.model.Annotation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnnotationRepository(private val store: JsonStore<LibraryState>) {

    fun observeForDocument(docUri: String): Flow<List<Annotation>> = store.state.map { state ->
        state.annotations
            .filter { it.docUri == docUri && !it.deleted }
            .sortedBy { it.selector.start }
    }

    fun observeAll(): Flow<List<Annotation>> = store.state.map { state ->
        state.annotations
            .filter { !it.deleted }
            .sortedByDescending { it.updatedAt }
    }

    suspend fun getForDocument(docUri: String): List<Annotation> =
        store.state.value.annotations.filter { it.docUri == docUri }

    suspend fun save(annotation: Annotation) = store.update { state ->
        val withoutSame = state.annotations.filterNot { it.id == annotation.id }
        state.copy(annotations = withoutSame + annotation)
    }

    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis()) = store.update { state ->
        state.copy(
            annotations = state.annotations.map { annotation ->
                if (annotation.id == id) annotation.copy(deleted = true, updatedAt = timestamp) else annotation
            },
        )
    }

    suspend fun updateOrphaned(annotation: Annotation, orphaned: Boolean) =
        save(annotation.copy(orphaned = orphaned))

    suspend fun reassign(oldUri: String, newUri: String) = store.update { state ->
        state.copy(
            annotations = state.annotations.map { annotation ->
                if (annotation.docUri == oldUri) annotation.copy(docUri = newUri) else annotation
            },
        )
    }

    suspend fun deleteForDocument(docUri: String) = store.update { state ->
        state.copy(annotations = state.annotations.filterNot { it.docUri == docUri })
    }

    /**
     * Merge imported annotations by id, keeping the newest `updatedAt`. Tombstones win so that a
     * deletion on one device is not resurrected by a stale copy elsewhere.
     */
    suspend fun merge(incoming: List<Annotation>): MergeResult {
        var added = 0
        var updated = 0
        var skipped = 0
        store.update { state ->
            val byId = state.annotations.associateBy { it.id }.toMutableMap()
            for (annotation in incoming) {
                val existing = byId[annotation.id]
                when {
                    existing == null -> {
                        byId[annotation.id] = annotation
                        added++
                    }
                    annotation.updatedAt > existing.updatedAt -> {
                        byId[annotation.id] = annotation
                        updated++
                    }
                    else -> skipped++
                }
            }
            state.copy(annotations = byId.values.toList())
        }
        return MergeResult(added, updated, skipped)
    }
}

data class MergeResult(val added: Int, val updated: Int, val skipped: Int)
