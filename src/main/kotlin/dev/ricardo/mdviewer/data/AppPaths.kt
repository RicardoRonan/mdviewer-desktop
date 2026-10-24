package dev.ricardo.mdviewer.data

import java.io.File

/** XDG-aware locations for the app's config and data files. */
object AppPaths {

    private val home: String get() = System.getProperty("user.home")

    private fun env(name: String, fallback: String): File =
        File(System.getenv(name)?.takeIf { it.isNotBlank() } ?: fallback)

    val configDir: File = File(env("XDG_CONFIG_HOME", "$home/.config"), "mdviewer")
    val dataDir: File = File(env("XDG_DATA_HOME", "$home/.local/share"), "mdviewer")

    val settingsFile: File = File(configDir, "settings.json")
    val libraryFile: File = File(dataDir, "library.json")

    /** Where new notes go when the library root is not configured. */
    val defaultNotesDir: File = File("$home/Documents/MDViewer")
}
