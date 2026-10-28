package dev.ricardo.mdviewer.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.ricardo.mdviewer.di.AppGraph
import dev.ricardo.mdviewer.domain.markdown.plainText
import dev.ricardo.mdviewer.domain.model.AnnotationKind
import dev.ricardo.mdviewer.ui.components.AppTopBar
import dev.ricardo.mdviewer.ui.components.CircleIconButton
import dev.ricardo.mdviewer.ui.components.EmptyState
import dev.ricardo.mdviewer.ui.components.PillButton
import dev.ricardo.mdviewer.ui.components.PillStyle
import dev.ricardo.mdviewer.ui.icons.Feather
import dev.ricardo.mdviewer.ui.platform.Dialogs
import dev.ricardo.mdviewer.ui.theme.HighlightColors
import dev.ricardo.mdviewer.ui.theme.HighlightYellow
import dev.ricardo.mdviewer.ui.theme.LocalSpacing
import dev.ricardo.mdviewer.ui.theme.LocalUberColors
import dev.ricardo.mdviewer.ui.theme.Motion
import dev.ricardo.mdviewer.ui.theme.motionTween
import dev.ricardo.mdviewer.ui.theme.parseHexColor
import dev.ricardo.mdviewer.ui.theme.rememberMotionEnabled
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@Composable
fun ReaderScreen(
    path: String,
    startInEditor: Boolean,
    graph: AppGraph,
    onBack: () -> Unit,
    onOpenNotebook: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(path, startInEditor) {
        ReaderViewModel(
            documentPath = path,
            startInEditor = startInEditor,
            documentRepository = graph.documentRepository,
            annotationRepository = graph.annotationRepository,
            settingsRepository = graph.settingsRepository,
            transfer = graph.transfer,
            scope = scope,
        )
    }

    val state by viewModel.state.collectAsState()
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    val motion = rememberMotionEnabled()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var noteTarget by remember { mutableStateOf<Pair<String, IntRange>?>(null) }
    var draft by remember { mutableStateOf(TextFieldValue("")) }
    var draftReady by remember { mutableStateOf(false) }
    var showFileMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var renameName by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }
    var activeSelection by remember { mutableStateOf<Pair<String, IntRange>?>(null) }

    fun leaveEditor() {
        scope.launch {
            if (state.dirty) {
                val saved = runCatching { viewModel.save() }
                if (saved.isFailure) {
                    snackbarHostState.showSnackbar(saved.exceptionOrNull()?.message ?: "Could not save")
                    return@launch
                }
            }
            viewModel.setEditing(false)
        }
    }

    fun export(kind: ExportKind) {
        showFileMenu = false
        scope.launch {
            val exported = runCatching {
                val bundle = viewModel.buildExport()
                val (name, content) = when (kind) {
                    ExportKind.JSON -> bundle.suggestedJsonName to bundle.json
                    ExportKind.DIGEST -> bundle.suggestedDigestName to bundle.digest
                }
                Dialogs.saveText(name, content)
            }
            exported.onSuccess { file ->
                snackbarHostState.showSnackbar("Exported ${file?.name ?: "annotations"}")
            }.onFailure { error ->
                snackbarHostState.showSnackbar(error.message ?: "Export failed")
            }
        }
    }

    fun importSidecar() {
        showFileMenu = false
        val file = Dialogs.openJson() ?: return
        scope.launch {
            val outcome = runCatching {
                val text = file.readText()
                viewModel.importSidecar(text)
            }
            outcome.onSuccess { result ->
                snackbarHostState.showSnackbar(
                    "Imported ${result.incomingCount} annotation(s): " +
                        "${result.merge?.added ?: 0} new, ${result.merge?.updated ?: 0} updated",
                )
            }.onFailure { error ->
                snackbarHostState.showSnackbar(error.message ?: "Import failed")
            }
        }
    }

    // Scroll restore has to wait until the column is measured. Doing it on the first
    // frame clamps to 0 and the next save wipes the place the reader stopped.
    var canSavePlace by remember(state.documentUri) { mutableStateOf(false) }
    LaunchedEffect(state.loading, state.documentUri, state.initialScroll) {
        if (state.loading || state.documentUri.isEmpty()) return@LaunchedEffect
        val target = state.initialScroll
        if (target > 0) {
            var frames = 0
            while (frames < 8 && scrollState.maxValue < target) {
                withFrameNanos { }
                frames++
            }
            scrollState.scrollTo(target.coerceAtMost(scrollState.maxValue))
        }
        canSavePlace = true
    }

    LaunchedEffect(state.loading, state.documentUri) {
        if (!state.loading && state.documentUri.isNotEmpty() && !draftReady) {
            draft = TextFieldValue(state.source)
            draftReady = true
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val readerMode = when {
        state.loading -> "loading"
        state.editing -> "edit"
        state.error != null -> "error"
        else -> "read"
    }

    // Desktop has no activity pause, so the reading place is saved after the scroll settles.
    LaunchedEffect(state.loading, state.documentUri, readerMode) {
        if (state.loading || state.documentUri.isEmpty() || readerMode != "read") return@LaunchedEffect
        snapshotFlow { scrollState.value }
            .debounce(600)
            .collect { position -> if (canSavePlace) viewModel.savePosition(position) }
    }

    DisposableEffect(state.documentUri) {
        onDispose {
            if (canSavePlace) viewModel.savePosition(scrollState.value)
        }
    }

    val highlights = remember(state.resolved) { buildHighlights(state.resolved) }
    val orphanCount = state.resolved.count { it.orphaned }
    val chapters = remember(state.blocks) { chapterOutline(state.blocks) }
    val blockOffsets = remember { mutableStateMapOf<String, Float>() }
    val railOffsetPx = with(LocalDensity.current) { 18.dp.toPx() }
    val activeChapterId = remember(scrollState.value, blockOffsets, chapters) {
        if (chapters.isEmpty()) return@remember null
        val scroll = scrollState.value.toFloat()
        val hits = chapters.mapNotNull { chapter ->
            val y = blockOffsets[chapter.blockId] ?: return@mapNotNull null
            if (y <= scroll + 80f) chapter.blockId to y else null
        }
        hits.maxByOrNull { it.second }?.first ?: chapters.first().blockId
    }

    // The action bar outlives the live text selection: a field collapses its own selection when it
    // loses focus, which must not pull the bar out from under the button being pressed.
    var heldSelection by remember { mutableStateOf<Pair<String, IntRange>?>(null) }
    if (activeSelection != null) heldSelection = activeSelection

    Box(
        Modifier
            .fillMaxSize()
            .background(uber.canvas)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && activeSelection != null) {
                    activeSelection = null
                    true
                } else {
                    false
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                title = state.documentName.ifBlank { "Reader" },
                onBack = if (state.editing) ::leaveEditor else onBack,
                actions = {
                    Box {
                        CircleIconButton(
                            icon = Feather.More,
                            contentDescription = "Options",
                            onClick = { showFileMenu = true },
                        )
                        DropdownMenu(
                            expanded = showFileMenu,
                            onDismissRequest = { showFileMenu = false },
                            containerColor = uber.canvasSoft,
                        ) {
                            if (state.editing) {
                                ReaderMenuItem("Save") {
                                    showFileMenu = false
                                    scope.launch {
                                        val saved = runCatching { viewModel.save() }
                                        snackbarHostState.showSnackbar(
                                            saved.exceptionOrNull()?.message ?: "Saved",
                                        )
                                    }
                                }
                                ReaderMenuItem("Preview") {
                                    showFileMenu = false
                                    leaveEditor()
                                }
                            } else {
                                ReaderMenuItem("Edit") {
                                    showFileMenu = false
                                    activeSelection = null
                                    if (!draftReady) draft = TextFieldValue(state.source)
                                    draftReady = true
                                    viewModel.setEditing(true)
                                }
                                ReaderMenuItem("Annotations") {
                                    showFileMenu = false
                                    onOpenNotebook()
                                }
                                HorizontalDivider(color = uber.surfacePressed)
                                ReaderMenuItem("Export JSON") { export(ExportKind.JSON) }
                                ReaderMenuItem("Export digest") { export(ExportKind.DIGEST) }
                                ReaderMenuItem("Import annotations") { importSidecar() }
                            }
                            HorizontalDivider(color = uber.surfacePressed)
                            ReaderMenuItem("Rename") {
                                showFileMenu = false
                                renameName = state.documentName
                                    .removeSuffix(".markdown")
                                    .removeSuffix(".md")
                                renameError = null
                                showRename = true
                            }
                            ReaderMenuItem("Delete") {
                                showFileMenu = false
                                showDelete = true
                            }
                        }
                    }
                },
            )

            Crossfade(
                targetState = readerMode,
                modifier = Modifier.weight(1f),
                animationSpec = motionTween(Motion.Base, motion),
                label = "reader",
            ) { mode ->
                when (mode) {
                    "loading" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = uber.ink)
                    }

                    "edit" -> MarkdownEditor(
                        value = draft,
                        fontScale = state.settings.fontScale,
                        onValueChange = { updated ->
                            draft = updated
                            viewModel.updateDraft(updated.text)
                        },
                    )

                    "error" -> EmptyState(
                        title = "Could not open document",
                        body = state.error ?: "",
                        action = { PillButton("Retry", onClick = viewModel::load) },
                    )

                    else -> Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(horizontal = spacing.xl),
                        ) {
                            if (orphanCount > 0) {
                                Spacer(Modifier.height(spacing.md))
                                Text(
                                    text = "$orphanCount annotation(s) could not be re-anchored after edits.",
                                    color = uber.body,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Spacer(Modifier.height(spacing.lg))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                MarkdownContent(
                                    blocks = state.blocks,
                                    highlights = highlights,
                                    fontScale = state.settings.fontScale,
                                    loadRemoteImages = state.settings.loadRemoteImages,
                                    clearSelection = activeSelection == null,
                                    onSelectionChange = { blockId, range ->
                                        activeSelection = if (range.isEmpty()) null else blockId to range
                                    },
                                    onLinkClick = { url ->
                                        val trimmed = url.trim()
                                        val allowed = trimmed.startsWith("https://", ignoreCase = true) ||
                                            trimmed.startsWith("http://", ignoreCase = true) ||
                                            trimmed.startsWith("mailto:", ignoreCase = true)
                                        if (allowed) runCatching { uriHandler.openUri(trimmed) }
                                    },
                                    modifier = Modifier.widthIn(max = state.settings.maxLineWidthDp.dp),
                                    onBlockPosition = { blockId, y ->
                                        blockOffsets[blockId] = y
                                    },
                                )
                            }
                            Spacer(Modifier.height(64.dp))
                        }
                        ChapterRail(
                            chapters = chapters,
                            activeId = activeChapterId,
                            onJump = { blockId ->
                                val target = blockOffsets[blockId] ?: return@ChapterRail
                                scope.launch {
                                    scrollState.animateScrollTo((target - railOffsetPx).coerceAtLeast(0f).toInt())
                                }
                            },
                            scrollState = scrollState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = spacing.xs),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !state.editing && activeSelection != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = spacing.lg, vertical = spacing.xl),
            enter = fadeIn(motionTween(Motion.Quick, motion)) + expandVertically(motionTween(Motion.Quick, motion)),
            exit = fadeOut(motionTween(Motion.Quick, motion)) + shrinkVertically(motionTween(Motion.Quick, motion)),
        ) {
            heldSelection?.let { selection ->
                val (blockId, range) = selection
                val blockText = state.blocks.firstOrNull { it.id == blockId }?.plainText().orEmpty()
                val start = range.first.coerceIn(0, blockText.length)
                val end = (range.last + 1).coerceIn(start, blockText.length)
                SelectionActionBar(
                    onHighlight = { color ->
                        viewModel.addHighlight(blockId, range, color)
                        activeSelection = null
                    },
                    onNote = {
                        noteTarget = blockId to range
                        activeSelection = null
                    },
                    onCopy = {
                        clipboard.setText(AnnotatedString(blockText.substring(start, end)))
                        scope.launch { snackbarHostState.showSnackbar("Copied") }
                        activeSelection = null
                    },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    noteTarget?.let { target ->
        val (blockId, range) = target
        val blockText = state.blocks.firstOrNull { it.id == blockId }
            ?.plainText()
            .orEmpty()
        val start = range.first.coerceIn(0, blockText.length)
        val end = (range.last + 1).coerceIn(start, blockText.length)
        NoteSheet(
            quote = blockText.substring(start, end),
            onDismiss = { noteTarget = null },
            onSave = { note, tags ->
                viewModel.addNote(blockId, range, note, tags)
                noteTarget = null
            },
        )
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            containerColor = uber.canvas,
            title = { Text("Rename", color = uber.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    TextField(
                        value = renameName,
                        onValueChange = { renameName = it },
                        singleLine = true,
                        placeholder = { Text("Name") },
                    )
                    if (renameError != null) {
                        Text(
                            text = renameError ?: "",
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
                            val renamed = runCatching { viewModel.rename(renameName) }
                            renamed.onSuccess { showRename = false }
                                .onFailure { error ->
                                    renameError = error.message ?: "Could not rename the file"
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
                    onClick = { showRename = false },
                )
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = uber.canvas,
            title = { Text("Delete file", color = uber.ink) },
            text = { Text("Delete ${state.documentName}? This removes the file from disk.", color = uber.body) },
            confirmButton = {
                PillButton(
                    text = "Delete",
                    compact = true,
                    onClick = {
                        scope.launch {
                            val deleted = runCatching { viewModel.deleteDocument() }
                            if (deleted.isSuccess) {
                                showDelete = false
                                onBack()
                            } else {
                                showDelete = false
                                snackbarHostState.showSnackbar(
                                    deleted.exceptionOrNull()?.message ?: "Could not delete the file",
                                )
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
                    onClick = { showDelete = false },
                )
            },
        )
    }
}

private enum class ExportKind { JSON, DIGEST }

private fun buildHighlights(resolved: List<ResolvedAnnotation>): Map<String, List<TextHighlight>> {
    return resolved
        .filter { !it.orphaned }
        .groupBy { it.blockId }
        .mapValues { (_, items) ->
            items.mapNotNull { item ->
                val start = item.range.first
                val end = item.range.last + 1
                if (end <= start) return@mapNotNull null
                val base = parseHexColor(item.annotation.color, HighlightYellow)
                val alpha = if (item.annotation.kind == AnnotationKind.NOTE) 0.65f else 0.72f
                TextHighlight(
                    start = start,
                    end = end,
                    color = base.copy(alpha = alpha),
                    note = item.annotation.note,
                    tags = item.annotation.tags,
                )
            }
        }
}

@Composable
private fun ReaderMenuItem(label: String, onClick: () -> Unit) {
    val uber = LocalUberColors.current
    DropdownMenuItem(
        text = {
            Text(text = label, color = uber.ink, style = MaterialTheme.typography.bodyLarge)
        },
        onClick = onClick,
    )
}

@Composable
private fun SelectionActionBar(
    onHighlight: (String) -> Unit,
    onNote: () -> Unit,
    onCopy: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val uber = LocalUberColors.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .clip(shape)
            .background(uber.canvas)
            .border(1.dp, uber.hairlineMid, shape)
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            HighlightColors.forEach { swatch ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(parseHexColor(swatch.hex, HighlightYellow))
                        .border(1.dp, uber.hairlineMid, CircleShape)
                        .semantics { contentDescription = "${swatch.name} highlight" }
                        .clickable { onHighlight(swatch.hex) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            PillButton(
                text = "Note",
                compact = true,
                onClick = onNote,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = "Copy",
                style = PillStyle.Subtle,
                compact = true,
                onClick = onCopy,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NoteSheet(
    quote: String,
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit,
) {
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    var note by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    val noteFocus = remember { FocusRequester() }

    // A dialog window can open without handing keyboard focus to the field inside it; without this
    // the note text silently goes nowhere. Retry for a couple of frames because the dialog content
    // is not attached on the first composition pass.
    LaunchedEffect(Unit) {
        repeat(3) {
            withFrameNanos { }
            if (runCatching { noteFocus.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = uber.canvas,
        title = { Text("Add note", style = MaterialTheme.typography.headlineSmall, color = uber.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                if (quote.isNotBlank()) {
                    Text(
                        text = "\u201C$quote\u201D",
                        style = MaterialTheme.typography.bodySmall,
                        color = uber.body,
                        maxLines = 3,
                    )
                }
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Your note") },
                    modifier = Modifier.fillMaxWidth().focusRequester(noteFocus),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = uber.canvasSoft,
                        unfocusedContainerColor = uber.canvasSoft,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = uber.ink,
                        focusedTextColor = uber.ink,
                        unfocusedTextColor = uber.ink,
                    ),
                )
                TextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    placeholder = { Text("Tags, comma separated") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = uber.canvasSoft,
                        unfocusedContainerColor = uber.canvasSoft,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = uber.ink,
                        focusedTextColor = uber.ink,
                        unfocusedTextColor = uber.ink,
                    ),
                )
            }
        },
        confirmButton = {
            PillButton(
                text = "Save",
                onClick = {
                    onSave(
                        note,
                        tagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                    )
                },
            )
        },
        dismissButton = {
            PillButton(text = "Cancel", style = PillStyle.Subtle, onClick = onDismiss)
        },
    )
}
