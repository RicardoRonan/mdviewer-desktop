package dev.ricardo.mdviewer.feature.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ricardo.mdviewer.data.scan.FolderChild
import dev.ricardo.mdviewer.data.scan.IndexedMarkdown
import dev.ricardo.mdviewer.data.scan.folderPage
import dev.ricardo.mdviewer.domain.model.Document
import dev.ricardo.mdviewer.ui.components.AppTopBar
import dev.ricardo.mdviewer.ui.components.CircleIconButton
import dev.ricardo.mdviewer.ui.components.ContentCard
import dev.ricardo.mdviewer.ui.components.EmptyState
import dev.ricardo.mdviewer.ui.components.PillButton
import dev.ricardo.mdviewer.ui.components.PillStyle
import dev.ricardo.mdviewer.ui.format.humanSize
import dev.ricardo.mdviewer.ui.format.relativeTime
import dev.ricardo.mdviewer.ui.icons.Feather
import dev.ricardo.mdviewer.ui.platform.Dialogs
import dev.ricardo.mdviewer.ui.theme.LocalSpacing
import dev.ricardo.mdviewer.ui.theme.LocalUberColors
import dev.ricardo.mdviewer.ui.theme.Motion
import dev.ricardo.mdviewer.ui.theme.motionTween
import dev.ricardo.mdviewer.ui.theme.rememberMotionEnabled
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpen: (String) -> Unit,
    onCreate: (String) -> Unit,
    onNotebook: () -> Unit,
    onSettings: () -> Unit,
) {
    val recents by viewModel.recents.collectAsState()
    val library by viewModel.library.collectAsState()
    val folderPath by viewModel.folderPath.collectAsState()
    val atRoot = folderPath.isEmpty()
    val spacing = LocalSpacing.current
    val uber = LocalUberColors.current
    val motion = rememberMotionEnabled()
    val scope = rememberCoroutineScope()

    var showNewNote by rememberSaveable { mutableStateOf(false) }
    var newNoteName by rememberSaveable { mutableStateOf("") }
    var newNoteError by rememberSaveable { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<FileTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<FileTarget?>(null) }
    var renameName by rememberSaveable { mutableStateOf("") }
    var fileError by rememberSaveable { mutableStateOf<String?>(null) }

    val rootDir = library.rootPath.takeIf { it.isNotBlank() }?.let(::File)

    fun openRename(path: String, name: String) {
        renameTarget = FileTarget(path, name)
        renameName = name.removeSuffix(".markdown").removeSuffix(".md")
        fileError = null
    }

    fun openDelete(path: String, name: String) {
        fileError = null
        deleteTarget = FileTarget(path, name)
    }

    val rootLabel = rootDir?.name ?: "MDViewer"
    val title = if (atRoot) rootLabel else folderPath.substringAfterLast('/')

    Column(Modifier.fillMaxSize().background(uber.canvas)) {
        AppTopBar(
            title = title,
            onBack = if (atRoot) null else viewModel::goUp,
            actions = {
                CircleIconButton(icon = Feather.BookOpen, contentDescription = "Notebook", onClick = onNotebook)
                CircleIconButton(icon = Feather.Settings, contentDescription = "Settings", onClick = onSettings)
            },
        )

        AnimatedContent(
            modifier = Modifier.weight(1f),
            targetState = folderPath,
            transitionSpec = {
                val forward = targetState.startsWith(initialState) && targetState != initialState
                val enter = fadeIn(motionTween(Motion.Base, motion)) +
                    slideInHorizontally(motionTween(Motion.Base, motion)) { full -> if (forward) full / 5 else -full / 5 }
                val exit = fadeOut(motionTween(Motion.Quick, motion)) +
                    slideOutHorizontally(motionTween(Motion.Quick, motion)) { full -> if (forward) -full / 8 else full / 8 }
                enter togetherWith exit
            },
            label = "folder",
        ) { shownPath ->
            val shownRoot = shownPath.isEmpty()
            val shownPage = folderPage(library.files, shownPath)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                val continueDoc = if (shownRoot) recents.firstOrNull() else null
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        PillButton(
                            text = "Open a file",
                            onClick = {
                                Dialogs.openMarkdown(rootDir)?.let { file -> onOpen(file.absolutePath) }
                            },
                        )
                        if (library.hasRoot) {
                            PillButton(
                                text = "New note",
                                style = PillStyle.Secondary,
                                onClick = {
                                    newNoteName = ""
                                    newNoteError = null
                                    showNewNote = true
                                },
                            )
                            if (shownRoot) {
                                PillButton(
                                    text = "Change folder",
                                    style = PillStyle.Subtle,
                                    onClick = {
                                        Dialogs.chooseFolder("Choose library folder", rootDir)?.let(viewModel::setRoot)
                                    },
                                )
                            }
                        }
                    }
                }

                if (continueDoc != null) {
                    item { SectionLabel(text = "CONTINUE READING") }
                    item {
                        ContinueCard(
                            document = continueDoc,
                            onClick = { onOpen(continueDoc.uri) },
                            onRename = { openRename(continueDoc.uri, continueDoc.name) },
                            onDelete = { openDelete(continueDoc.uri, continueDoc.name) },
                        )
                    }
                }

                when {
                    !library.checked -> item { SectionLabel(text = "SEARCHING") }

                    !library.hasRoot -> item {
                        EmptyState(
                            title = "Choose a library folder",
                            body = "Pick a folder of Markdown files and MDViewer will browse it here. " +
                                "You can also open any single file from disk.",
                            action = {
                                PillButton(
                                    text = "Choose folder",
                                    onClick = {
                                        Dialogs.chooseFolder("Choose library folder", rootDir)?.let(viewModel::setRoot)
                                    },
                                )
                            },
                        )
                    }

                    library.scanning && library.files.isEmpty() -> item { SectionLabel(text = "SEARCHING") }

                    shownPage.folders.isEmpty() && shownPage.files.isEmpty() -> item {
                        Text(
                            text = if (shownRoot) {
                                "No Markdown files in this folder."
                            } else {
                                "This folder is empty."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = uber.body,
                        )
                    }

                    else -> {
                        if (shownPage.folders.isNotEmpty()) {
                            item { SectionLabel(text = "FOLDERS  ·  ${shownPage.folders.size}") }
                            items(shownPage.folders, key = { "dir:${it.path}" }) { folder ->
                                FolderCard(folder = folder, onClick = { viewModel.openFolder(folder.path) })
                            }
                        }
                        if (shownPage.files.isNotEmpty()) {
                            item { SectionLabel(text = "FILES  ·  ${shownPage.files.size}") }
                            items(shownPage.files, key = { it.openUri }) { file ->
                                FileCard(
                                    file = file,
                                    onClick = { onOpen(file.openUri) },
                                    onRename = { openRename(file.openUri, file.name) },
                                    onDelete = { openDelete(file.openUri, file.name) },
                                )
                            }
                        }
                    }
                }

                val history = if (continueDoc == null) recents else recents.drop(1)
                if (shownRoot && history.isNotEmpty()) {
                    item { SectionLabel(text = "RECENT") }
                    items(history, key = { "recent:${it.uri}" }) { document ->
                        RecentCard(
                            document = document,
                            onClick = { onOpen(document.uri) },
                            onRename = { openRename(document.uri, document.name) },
                            onDelete = { openDelete(document.uri, document.name) },
                        )
                    }
                }
            }
        }
    }

    if (showNewNote) {
        AlertDialog(
            onDismissRequest = { showNewNote = false },
            containerColor = uber.canvas,
            title = { Text("New note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    TextField(
                        value = newNoteName,
                        onValueChange = { newNoteName = it },
                        singleLine = true,
                        placeholder = { Text("Name") },
                    )
                    if (newNoteError != null) {
                        Text(
                            text = newNoteError ?: "",
                            color = uber.body,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                PillButton(
                    text = "Create",
                    compact = true,
                    onClick = {
                        scope.launch {
                            val created = runCatching { viewModel.createNote(newNoteName, folderPath) }
                            created.onSuccess { uri ->
                                showNewNote = false
                                viewModel.refresh()
                                onCreate(uri)
                            }.onFailure { error ->
                                newNoteError = error.message ?: "Could not create the note"
                            }
                        }
                    },
                )
            },
            dismissButton = {
                PillButton(
                    text = "Cancel",
                    style = PillStyle.Subtle,
                    compact = true,
                    onClick = { showNewNote = false },
                )
            },
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = uber.canvas,
            title = { Text("Rename") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    TextField(
                        value = renameName,
                        onValueChange = { renameName = it },
                        singleLine = true,
                        placeholder = { Text("Name") },
                    )
                    if (fileError != null) {
                        Text(
                            text = fileError ?: "",
                            color = uber.body,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                PillButton(
                    text = "Rename",
                    compact = true,
                    onClick = {
                        scope.launch {
                            val renamed = runCatching { viewModel.renameFile(target.path, renameName) }
                            renamed.onSuccess {
                                renameTarget = null
                                viewModel.refresh()
                            }.onFailure { error ->
                                fileError = error.message ?: "Could not rename the file"
                            }
                        }
                    },
                )
            },
            dismissButton = {
                PillButton(
                    text = "Cancel",
                    style = PillStyle.Subtle,
                    compact = true,
                    onClick = { renameTarget = null },
                )
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = uber.canvas,
            title = { Text("Delete file") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text("Delete ${target.name}? This removes the file from disk.")
                    if (fileError != null) {
                        Text(
                            text = fileError ?: "",
                            color = uber.body,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                PillButton(
                    text = "Delete",
                    compact = true,
                    onClick = {
                        scope.launch {
                            val deleted = runCatching { viewModel.deleteFile(target.path) }
                            deleted.onSuccess {
                                deleteTarget = null
                                viewModel.refresh()
                            }.onFailure { error ->
                                fileError = error.message ?: "Could not delete the file"
                            }
                        }
                    },
                )
            },
            dismissButton = {
                PillButton(
                    text = "Cancel",
                    style = PillStyle.Subtle,
                    compact = true,
                    onClick = { deleteTarget = null },
                )
            },
        )
    }
}

private data class FileTarget(val path: String, val name: String)

@Composable
private fun SectionLabel(text: String) {
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = uber.body,
        modifier = Modifier.padding(top = spacing.lg),
    )
}

@Composable
private fun ContinueCard(
    document: Document,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    ContentCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = uber.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (document.lastPosition > 0) {
                        "Where you left off"
                    } else {
                        "Last opened ${relativeTime(document.lastOpened)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = uber.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FileActions(onRename, onDelete)
        }
    }
}

@Composable
private fun FolderCard(folder: FolderChild, onClick: () -> Unit) {
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    ContentCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Icon(
                imageVector = Feather.Folder,
                contentDescription = null,
                tint = uber.ink,
                modifier = Modifier.size(22.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = uber.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = fileCountLabel(folder.markdownCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = uber.body,
                )
            }
            Icon(
                imageVector = Feather.ChevronRight,
                contentDescription = null,
                tint = uber.mute,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun FileCard(
    file: IndexedMarkdown,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    ContentCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Icon(
                imageVector = Feather.File,
                contentDescription = null,
                tint = uber.ink,
                modifier = Modifier.size(22.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = uber.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${humanSize(file.size)}  ·  edited ${relativeTime(file.modifiedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = uber.body,
                )
            }
            FileActions(onRename, onDelete)
        }
    }
}

@Composable
private fun RecentCard(
    document: Document,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    ContentCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = uber.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        text = humanSize(document.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = uber.body,
                    )
                    Text(
                        text = "\u00B7",
                        style = MaterialTheme.typography.bodySmall,
                        color = uber.mute,
                    )
                    Text(
                        text = relativeTime(document.lastOpened),
                        style = MaterialTheme.typography.bodySmall,
                        color = uber.body,
                    )
                }
            }
            FileActions(onRename, onDelete)
        }
    }
}

@Composable
private fun FileActions(onRename: () -> Unit, onDelete: () -> Unit) {
    val uber = LocalUberColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CardIcon(Feather.Edit, "Rename", uber.ink, onRename)
        CardIcon(Feather.Trash, "Delete", uber.ink, onDelete)
    }
}

@Composable
private fun CardIcon(
    icon: ImageVector,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
    )
}

private fun fileCountLabel(count: Int): String = if (count == 1) "1 file" else "$count files"
