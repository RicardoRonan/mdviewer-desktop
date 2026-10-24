package dev.ricardo.mdviewer.ui.platform

import dev.ricardo.mdviewer.data.scan.MARKDOWN_EXTENSIONS
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Native file pickers. Compose Desktop runs the UI on the AWT event thread, so the Swing choosers
 * can be shown directly without extra dispatching.
 */
object Dialogs {

    fun openMarkdown(start: File? = null): File? = openFile(
        title = "Open Markdown file",
        start = start,
        filter = FileNameExtensionFilter(
            "Markdown (${MARKDOWN_EXTENSIONS.joinToString(", ") { "*.$it" }})",
            *MARKDOWN_EXTENSIONS.toTypedArray(),
        ),
    )

    fun openJson(start: File? = null): File? = openFile(
        title = "Import annotations",
        start = start,
        filter = FileNameExtensionFilter("Annotation JSON (*.json)", "json"),
    )

    fun chooseFolder(title: String, start: File? = null): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
            start?.let { currentDirectory = it }
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }

    fun saveText(suggestedName: String, content: String, start: File? = null): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = "Save export"
            selectedFile = File(start, suggestedName)
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null
        val file = chooser.selectedFile
        runCatching { file.writeText(content) }
            .getOrElse { throw IllegalStateException("Could not write ${file.name}: ${it.message}") }
        return file
    }

    private fun openFile(title: String, start: File?, filter: FileNameExtensionFilter?): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.FILES_ONLY
            filter?.let { fileFilter = it }
            start?.let { currentDirectory = if (it.isDirectory) it else it.parentFile }
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }
}
