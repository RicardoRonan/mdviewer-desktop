package dev.ricardo.mdviewer

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.ricardo.mdviewer.di.AppGraph
import java.awt.Dimension
import java.io.File

fun main(args: Array<String>) {
    val graph = AppGraph()
    val initialPath = args
        .asSequence()
        .mapNotNull { argument -> runCatching { File(argument).canonicalFile }.getOrNull() }
        .firstOrNull { it.isFile }
        ?.path

    application {
        val windowState = rememberWindowState(width = 1120.dp, height = 820.dp)
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "MDViewer",
        ) {
            LaunchedEffect(Unit) {
                window.minimumSize = Dimension(760, 520)
            }
            App(graph, initialPath)
        }
    }
}
