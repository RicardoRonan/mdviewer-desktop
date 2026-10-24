package dev.ricardo.mdviewer.feature.settings

import dev.ricardo.mdviewer.data.prefs.SettingsRepository
import dev.ricardo.mdviewer.domain.model.AppTheme
import dev.ricardo.mdviewer.domain.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
) {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun setTheme(theme: AppTheme) {
        scope.launch { settingsRepository.setTheme(theme) }
    }

    fun setFontScale(scale: Float) {
        scope.launch { settingsRepository.setFontScale(scale) }
    }

    fun setAccentHighlight(enabled: Boolean) {
        scope.launch { settingsRepository.setAccentHighlight(enabled) }
    }

    fun setLoadRemoteImages(enabled: Boolean) {
        scope.launch { settingsRepository.setLoadRemoteImages(enabled) }
    }

    fun setMaxLineWidth(dp: Int) {
        scope.launch { settingsRepository.setMaxLineWidth(dp) }
    }
}
