package dev.ricardo.mdviewer

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.ricardo.mdviewer.di.AppGraph
import dev.ricardo.mdviewer.domain.model.AppTheme
import dev.ricardo.mdviewer.feature.library.LibraryScreen
import dev.ricardo.mdviewer.feature.library.LibraryViewModel
import dev.ricardo.mdviewer.feature.notebook.NotebookScreen
import dev.ricardo.mdviewer.feature.notebook.NotebookViewModel
import dev.ricardo.mdviewer.feature.reader.ReaderScreen
import dev.ricardo.mdviewer.feature.settings.SettingsScreen
import dev.ricardo.mdviewer.feature.settings.SettingsViewModel
import dev.ricardo.mdviewer.ui.theme.MdViewerTheme

sealed interface Route {
    data object Library : Route
    data class Reader(val path: String, val edit: Boolean = false) : Route
    data object Notebook : Route
    data object Settings : Route
}

@Composable
fun App(graph: AppGraph, initialPath: String?) {
    val settings by graph.settingsRepository.settings
        .collectAsState(initial = graph.settingsRepository.current())
    val darkTheme = when (settings.theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    MdViewerTheme(darkTheme = darkTheme) {
        val scope = rememberCoroutineScope()
        val backStack = remember { mutableStateListOf<Route>(Route.Library) }
        val libraryViewModel = remember {
            LibraryViewModel(
                settingsRepository = graph.settingsRepository,
                documentRepository = graph.documentRepository,
                markdownIndex = graph.markdownIndex,
                scope = scope,
            )
        }
        val notebookViewModel = remember {
            NotebookViewModel(graph.annotationRepository, graph.documentRepository, scope)
        }
        val settingsViewModel = remember {
            SettingsViewModel(graph.settingsRepository, scope)
        }

        var initialHandled by remember { mutableStateOf(false) }
        LaunchedEffect(initialPath) {
            if (!initialHandled) {
                initialHandled = true
                if (!initialPath.isNullOrBlank()) backStack.add(Route.Reader(initialPath))
            }
        }

        fun pop() {
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            if (backStack.last() == Route.Library) libraryViewModel.refresh()
        }

        Crossfade(
            targetState = backStack.last(),
            modifier = Modifier.fillMaxSize(),
            animationSpec = tween(durationMillis = 180),
            label = "route",
        ) { route ->
            when (route) {
                Route.Library -> LibraryScreen(
                    viewModel = libraryViewModel,
                    onOpen = { path -> backStack.add(Route.Reader(path)) },
                    onCreate = { path -> backStack.add(Route.Reader(path, edit = true)) },
                    onNotebook = { backStack.add(Route.Notebook) },
                    onSettings = { backStack.add(Route.Settings) },
                )

                is Route.Reader -> ReaderScreen(
                    path = route.path,
                    startInEditor = route.edit,
                    graph = graph,
                    onBack = ::pop,
                    onOpenNotebook = { backStack.add(Route.Notebook) },
                )

                Route.Notebook -> NotebookScreen(
                    viewModel = notebookViewModel,
                    onBack = ::pop,
                    onOpenDocument = { path -> backStack.add(Route.Reader(path)) },
                )

                Route.Settings -> SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = ::pop,
                )
            }
        }
    }
}
