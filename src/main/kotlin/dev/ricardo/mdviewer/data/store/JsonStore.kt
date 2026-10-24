package dev.ricardo.mdviewer.data.store

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Small JSON-file backed store. The data volume (a document index and a reader's annotations) is a
 * few megabytes at most, so an atomic rewrite per mutation is simpler and safer than an embedded
 * database, and it keeps the desktop build free of native SQLite packaging.
 */
class JsonStore<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val defaultValue: () -> T,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val mutex = Mutex()
    private val _state = MutableStateFlow(load())
    val state: StateFlow<T> = _state.asStateFlow()

    /** Applies [transform] and persists the result. Reads inside the transform see the latest state. */
    suspend fun update(transform: (T) -> T) {
        mutex.withLock {
            val next = transform(_state.value)
            if (next == _state.value) return
            _state.value = next
            write(next)
        }
    }

    private fun load(): T = runCatching {
        if (file.isFile) json.decodeFromString(serializer, file.readText()) else defaultValue()
    }.getOrElse { defaultValue() }

    private suspend fun write(value: T) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(json.encodeToString(serializer, value))
        if (!temp.renameTo(file)) {
            file.writeText(temp.readText())
            temp.delete()
        }
    }
}
