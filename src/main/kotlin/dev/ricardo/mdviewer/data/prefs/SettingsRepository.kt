package dev.ricardo.mdviewer.data.prefs

import dev.ricardo.mdviewer.data.store.JsonStore
import dev.ricardo.mdviewer.domain.model.AppTheme
import dev.ricardo.mdviewer.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** Settings persisted as JSON under the XDG config directory. */
class SettingsRepository(private val store: JsonStore<UserSettings>) {

    val settings: Flow<UserSettings> = store.state

    fun current(): UserSettings = store.state.value

    suspend fun setTheme(theme: AppTheme) = store.update { it.copy(theme = theme) }

    suspend fun setFontScale(scale: Float) = store.update { it.copy(fontScale = scale) }

    suspend fun setAccentHighlight(enabled: Boolean) =
        store.update { it.copy(accentHighlight = enabled) }

    suspend fun setLoadRemoteImages(enabled: Boolean) =
        store.update { it.copy(loadRemoteImages = enabled) }

    suspend fun setMaxLineWidth(dp: Int) = store.update { it.copy(maxLineWidthDp = dp) }

    suspend fun setLibraryRoot(path: String) = store.update { it.copy(libraryRoot = path) }
}
