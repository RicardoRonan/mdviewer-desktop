package dev.ricardo.mdviewer.data

import java.io.File

/**
 * Platform-aware locations for the app's config and data files.
 *
 * - Windows: `%APPDATA%\mdviewer` (config) and `%LOCALAPPDATA%\mdviewer` (data).
 * - Everything else: XDG (`$XDG_CONFIG_HOME/mdviewer`, `$XDG_DATA_HOME/mdviewer`).
 */
object AppPaths {

    private val home: String get() = System.getProperty("user.home")

    private fun env(name: String, fallback: String): File =
        File(System.getenv(name)?.takeIf { it.isNotBlank() } ?: fallback)

    val isWindows: Boolean get() =
        System.getProperty("os.name").lowercase().contains("win")

    val configDir: File = File(
        if (isWindows) env("APPDATA", "$home/AppData/Roaming") else env("XDG_CONFIG_HOME", "$home/.config"),
        "mdviewer",
    )

    val dataDir: File = File(
        if (isWindows) env("LOCALAPPDATA", "$home/AppData/Local") else env("XDG_DATA_HOME", "$home/.local/share"),
        "mdviewer",
    )

    val settingsFile: File = File(configDir, "settings.json")
    val libraryFile: File = File(dataDir, "library.json")

    /** Where new notes go when the library root is not configured. */
    val defaultNotesDir: File = File("$home/Documents/MDViewer")
}
