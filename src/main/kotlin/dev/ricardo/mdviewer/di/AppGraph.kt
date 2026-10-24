package dev.ricardo.mdviewer.di

import dev.ricardo.mdviewer.data.AppPaths
import dev.ricardo.mdviewer.data.prefs.SettingsRepository
import dev.ricardo.mdviewer.data.repo.AnnotationRepository
import dev.ricardo.mdviewer.data.repo.DocumentRepository
import dev.ricardo.mdviewer.data.scan.MarkdownIndex
import dev.ricardo.mdviewer.data.store.JsonStore
import dev.ricardo.mdviewer.data.store.LibraryState
import dev.ricardo.mdviewer.data.io.AnnotationTransfer
import dev.ricardo.mdviewer.domain.model.UserSettings
import java.io.File

/**
 * Hand-wired object graph. The app has six singletons and no scopes, so a DI framework would only
 * add build time and indirection.
 */
class AppGraph(
    configDir: File? = null,
    dataDir: File? = null,
) {
    private val configRoot = configDir ?: AppPaths.configDir
    private val dataRoot = dataDir ?: AppPaths.dataDir

    private val settingsStore = JsonStore(
        file = File(configRoot, "settings.json"),
        serializer = UserSettings.serializer(),
        defaultValue = { UserSettings() },
    )

    private val libraryStore = JsonStore(
        file = File(dataRoot, "library.json"),
        serializer = LibraryState.serializer(),
        defaultValue = { LibraryState() },
    )

    val settingsRepository = SettingsRepository(settingsStore)
    val markdownIndex = MarkdownIndex()
    val annotationRepository = AnnotationRepository(libraryStore)
    val documentRepository = DocumentRepository(libraryStore, annotationRepository, markdownIndex)
    val transfer = AnnotationTransfer(annotationRepository, documentRepository)
}
