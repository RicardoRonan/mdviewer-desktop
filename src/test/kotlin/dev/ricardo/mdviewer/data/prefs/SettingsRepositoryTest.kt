package dev.ricardo.mdviewer.data.prefs

import dev.ricardo.mdviewer.data.store.JsonStore
import dev.ricardo.mdviewer.domain.model.AppTheme
import dev.ricardo.mdviewer.domain.model.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsRepositoryTest {

    private fun newRepo(dir: File): SettingsRepository =
        SettingsRepository(JsonStore(File(dir, "settings.json"), UserSettings.serializer()) { UserSettings() })

    @Test
    fun `defaults match the reader defaults`() = runTest {
        val dir = File.createTempFile("settings", "").apply { delete(); mkdirs() }
        try {
            val settings = newRepo(dir).settings.first()
            assertEquals(AppTheme.SYSTEM, settings.theme)
            assertEquals(1f, settings.fontScale)
            assertTrue(settings.accentHighlight)
            assertFalse(settings.loadRemoteImages)
            assertEquals(700, settings.maxLineWidthDp)
            assertEquals("", settings.libraryRoot)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `setters persist across instances`() = runTest {
        val dir = File.createTempFile("settings", "").apply { delete(); mkdirs() }
        try {
            val repo = newRepo(dir)
            repo.setTheme(AppTheme.DARK)
            repo.setFontScale(1.15f)
            repo.setAccentHighlight(false)
            repo.setLoadRemoteImages(true)
            repo.setMaxLineWidth(560)
            repo.setLibraryRoot("/home/ricardo/Notes")

            val reloaded = newRepo(dir).settings.first()
            assertEquals(AppTheme.DARK, reloaded.theme)
            assertEquals(1.15f, reloaded.fontScale)
            assertFalse(reloaded.accentHighlight)
            assertTrue(reloaded.loadRemoteImages)
            assertEquals(560, reloaded.maxLineWidthDp)
            assertEquals("/home/ricardo/Notes", reloaded.libraryRoot)
        } finally {
            dir.deleteRecursively()
        }
    }
}
