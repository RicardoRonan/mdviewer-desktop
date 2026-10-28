package dev.ricardo.mdviewer.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ricardo.mdviewer.domain.markdown.MdBlock
import dev.ricardo.mdviewer.domain.markdown.plainText
import dev.ricardo.mdviewer.ui.theme.LocalSpacing
import dev.ricardo.mdviewer.ui.theme.LocalUberColors
import dev.ricardo.mdviewer.ui.theme.MdBodyMedium
import dev.ricardo.mdviewer.ui.theme.MdCaption
import dev.ricardo.mdviewer.ui.theme.UberColors

/** One entry in the chapter rail. [blockId] matches the parsed [MdBlock.Heading]. */
data class Chapter(
    val blockId: String,
    val level: Int,
    val title: String,
)

/** Headings in document order, ready for the rail. Blank headings are skipped. */
fun chapterOutline(blocks: List<MdBlock>): List<Chapter> = blocks
    .mapNotNull { block ->
        val heading = block as? MdBlock.Heading ?: return@mapNotNull null
        Chapter(
            blockId = heading.id,
            level = heading.level.coerceIn(1, 6),
            title = heading.plainText().trim(),
        )
    }
    .filter { it.title.isNotBlank() }

/**
 * Always-visible tick rail down the left edge of the reader. Hovering the ticks opens a panel
 * listing every heading in the document; picking one scrolls the reader to it.
 *
 * The container is wide enough for the open panel but carries no pointer handler of its own, so
 * text underneath stays selectable. Only the narrow tick strip takes input until the panel opens.
 */
@Composable
fun ChapterRail(
    chapters: List<Chapter>,
    activeId: String?,
    onJump: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chapters.isEmpty()) return

    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    val railHover = remember { MutableInteractionSource() }
    val expanded by railHover.collectIsHoveredAsState()
    val tickWidth = 18.dp
    val panelWidth = 320.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(panelWidth),
    ) {
        val panelMaxHeight = maxHeight * 0.7f

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(tickWidth)
                .hoverable(railHover)
                .semantics { contentDescription = "Chapters" },
        ) {
            ChapterTicks(
                chapters = chapters,
                activeId = activeId,
                uber = uber,
                modifier = Modifier.fillMaxHeight().fillMaxWidth(),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = tickWidth + spacing.sm),
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
        ) {
            ChapterPanel(
                chapters = chapters,
                activeId = activeId,
                onJump = onJump,
                uber = uber,
                modifier = Modifier.heightIn(max = panelMaxHeight),
            )
        }
    }
}

@Composable
private fun ChapterTicks(
    chapters: List<Chapter>,
    activeId: String?,
    uber: UberColors,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val margin = 28.dp
        val usable = (maxHeight - margin * 2).coerceAtLeast(1.dp)
        chapters.forEachIndexed { index, chapter ->
            val fraction = if (chapters.size == 1) 0.5f else index / (chapters.size - 1f)
            val top = margin + usable * fraction
            val thickness = if (chapter.level <= 2) 3.dp else 2.dp
            Box(
                modifier = Modifier
                    .offset(y = top)
                    .width(tickWidthFor(chapter.level))
                    .height(thickness)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (chapter.blockId == activeId) uber.ink else uber.hairlineMid.copy(alpha = 0.55f),
                    ),
            )
        }
    }
}

private fun tickWidthFor(level: Int) = when (level) {
    1 -> 16.dp
    2 -> 12.dp
    3 -> 8.dp
    else -> 5.dp
}

@Composable
private fun ChapterPanel(
    chapters: List<Chapter>,
    activeId: String?,
    onJump: (String) -> Unit,
    uber: UberColors,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .width(300.dp)
            .clip(shape)
            .background(uber.canvas)
            .border(1.dp, uber.hairlineMid.copy(alpha = 0.45f), shape)
            .padding(vertical = spacing.sm),
    ) {
        Text(
            text = "CHAPTERS",
            style = MdCaption,
            color = uber.mute,
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs),
        )
        LazyColumn {
            items(chapters, key = { it.blockId }) { chapter ->
                ChapterRow(
                    chapter = chapter,
                    active = chapter.blockId == activeId,
                    onClick = { onJump(chapter.blockId) },
                    uber = uber,
                )
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: Chapter,
    active: Boolean,
    onClick: () -> Unit,
    uber: UberColors,
) {
    val spacing = LocalSpacing.current
    val rowHover = remember { MutableInteractionSource() }
    val hovered by rowHover.collectIsHoveredAsState()
    val indent = spacing.lg + ((chapter.level - 1) * 12).dp
    Text(
        text = chapter.title,
        style = MdBodyMedium,
        color = if (active) uber.ink else uber.body,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 2,
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(rowHover)
            .clickable(onClick = onClick)
            .background(if (hovered) uber.canvasSoft else Color.Transparent)
            .padding(start = indent, end = spacing.lg, top = spacing.sm, bottom = spacing.sm)
            .semantics {
                contentDescription = "Go to ${chapter.title}"
                selected = active
            },
    )
}
