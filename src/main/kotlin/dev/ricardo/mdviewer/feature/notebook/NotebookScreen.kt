package dev.ricardo.mdviewer.feature.notebook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import dev.ricardo.mdviewer.domain.model.AnnotationKind
import dev.ricardo.mdviewer.ui.components.AppTopBar
import dev.ricardo.mdviewer.ui.components.CategoryChip
import dev.ricardo.mdviewer.ui.components.CircleIconButton
import dev.ricardo.mdviewer.ui.icons.Feather
import dev.ricardo.mdviewer.ui.components.ContentCard
import dev.ricardo.mdviewer.ui.components.EmptyState
import dev.ricardo.mdviewer.ui.theme.LocalSpacing
import dev.ricardo.mdviewer.ui.theme.LocalUberColors

@Composable
fun NotebookScreen(
    onBack: () -> Unit,
    onOpenDocument: (String) -> Unit,
    viewModel: NotebookViewModel,
) {
    val items by viewModel.items.collectAsState()
    val filter by viewModel.activeFilter.collectAsState()
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current

    Column(Modifier.fillMaxSize().background(uber.canvas)) {
        AppTopBar(title = "Notebook", onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            CategoryChip("All", selected = filter == null, onClick = { viewModel.setFilter(null) })
            CategoryChip(
                "Highlights",
                selected = filter == AnnotationKind.HIGHLIGHT,
                onClick = { viewModel.setFilter(AnnotationKind.HIGHLIGHT) },
            )
            CategoryChip(
                "Notes",
                selected = filter == AnnotationKind.NOTE,
                onClick = { viewModel.setFilter(AnnotationKind.NOTE) },
            )
        }

        Box(Modifier.fillMaxSize()) {
            if (items.isEmpty()) {
                EmptyState(
                    title = "Nothing here yet",
                    body = "Highlights and notes you create while reading show up here.",
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    items(items, key = { it.annotation.id }) { item ->
                        NotebookCard(
                            item = item,
                            onClick = { onOpenDocument(item.annotation.docUri) },
                            onDelete = { viewModel.delete(item.annotation.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotebookCard(item: NotebookItem, onClick: () -> Unit, onDelete: () -> Unit) {
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    ContentCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.annotation.kind.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = uber.body,
                )
                CircleIconButton(icon = Feather.Trash, contentDescription = "Delete", onClick = onDelete)
            }
            Text(
                text = "\u201C${item.annotation.exact}\u201D",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = uber.ink,
            )
            if (item.annotation.note.isNotBlank()) {
                Text(
                    text = item.annotation.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = uber.body,
                )
            }
            Text(
                text = item.docName,
                style = MaterialTheme.typography.labelSmall,
                color = uber.mute,
            )
            if (item.annotation.tags.isNotEmpty()) {
                Text(
                    text = item.annotation.tags.joinToString("  ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = uber.body,
                )
            }
        }
    }
}

private fun AnnotationKind.label(): String = when (this) {
    AnnotationKind.HIGHLIGHT -> "HIGHLIGHT"
    AnnotationKind.NOTE -> "NOTE"
    AnnotationKind.BOOKMARK -> "BOOKMARK"
}
