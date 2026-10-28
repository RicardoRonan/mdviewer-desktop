package dev.ricardo.mdviewer.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * The window of at most [limit] chapters shown in the panel at once. The window slides so the
 * active chapter stays visible as the reader scrolls, which keeps the menu short on long documents.
 */
fun visibleChapters(
    chapters: List<Chapter>,
    activeId: String?,
    limit: Int = 5,
): List<Chapter> {
    if (chapters.size <= limit) return chapters
    val activeIndex = chapters.indexOfFirst { it.blockId == activeId }
    if (activeIndex < 0) return chapters.take(limit)
    val start = (activeIndex - limit / 2).coerceIn(0, chapters.size - limit)
    return chapters.subList(start, start + limit)
}

/**
 * Always-visible tick rail down the right edge of the reader. Hovering the ticks opens a panel
 * listing every heading in the document; picking one scrolls the reader to it.
 *
 * The container is wide enough for the open panel but carries no pointer handler of its own, so
 * text underneath stays selectable. Only the narrow tick strip takes input until the panel opens.
 * The panel opens to the left of the ticks so it doesn't cover the rail itself and stays hoverable
 * when moving the mouse from the ticks into the menu. Only a short window of chapters is listed at
 * once (see [visibleChapters]) and it slides as the reader scrolls.
 *
 * The whole rail is a compact block, [railWidth] wide and no taller than [railMaxHeight], so it
 * never takes over the reader area.
 *
 * The strip doubles as a real [VerticalScrollbar]: dragging it scrubs the document using the
 * framework's proven adapter, while the ticks overlaid on top keep encoding heading depth and the
 * current section. Keyboard users get the same panel: focusing the strip opens it, Up/Down moves
 * between headings and jumps, and Escape closes it.
 */
@Composable
fun ChapterRail(
    chapters: List<Chapter>,
    activeId: String?,
    onJump: (String) -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    if (chapters.isEmpty()) return

    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    val railHover = remember { MutableInteractionSource() }
    val panelHover = remember { MutableInteractionSource() }
    val tickHovered by railHover.collectIsHoveredAsState()
    val panelHovered by panelHover.collectIsHoveredAsState()
    var keyboardOpen by remember { mutableStateOf(false) }
    val expanded = tickHovered || panelHovered || keyboardOpen
    val tickWidth = 14.dp
    val railWidth = 150.dp
    val railMaxHeight = 300.dp

    // Arrow keys move one heading at a time. The forced index keeps repeated presses snappy while
    // the scroll animation (and therefore activeId) is still catching up.
    val activeIndex = chapters.indexOfFirst { it.blockId == activeId }
    var forcedIndex by remember(chapters) { mutableStateOf(-1) }
    LaunchedEffect(activeId, chapters) { forcedIndex = -1 }
    fun moveBy(delta: Int) {
        val base = forcedIndex.takeIf { it >= 0 } ?: activeIndex.takeIf { it >= 0 } ?: 0
        val target = (base + delta).coerceIn(0, chapters.lastIndex)
        forcedIndex = target
        onJump(chapters[target].blockId)
    }

    BoxWithConstraints(
        modifier = modifier
            .width(railWidth)
            .heightIn(max = railMaxHeight)
            .fillMaxHeight(),
    ) {
        val panelMaxHeight = maxHeight * 0.85f

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(tickWidth)
                .align(Alignment.CenterEnd)
                .hoverable(railHover)
                .onFocusChanged { focus -> keyboardOpen = focus.isFocused }
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Escape -> {
                            keyboardOpen = false
                            true
                        }

                        Key.DirectionDown -> {
                            moveBy(1)
                            true
                        }

                        Key.DirectionUp -> {
                            moveBy(-1)
                            true
                        }

                        else -> false
                    }
                }
                .semantics { contentDescription = "Chapters" },
        ) {
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                style = ScrollbarStyle(
                    minimalHeight = 8.dp,
                    thickness = 5.dp,
                    shape = RoundedCornerShape(3.dp),
                    hoverColor = uber.hairlineMid.copy(alpha = 0.75f),
                    unhoverColor = uber.hairlineMid.copy(alpha = 0.4f),
                    hoverDurationMillis = 120,
                ),
            )
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
                .align(Alignment.CenterEnd)
                .padding(end = tickWidth + spacing.xs)
                .hoverable(panelHover),
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            ChapterPanel(
                chapters = chapters,
                activeId = activeId,
                onJump = onJump,
                uber = uber,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = panelMaxHeight),
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
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(uber.canvas)
            .border(1.dp, uber.hairlineMid.copy(alpha = 0.45f), shape)
            .padding(vertical = spacing.xs),
    ) {
        Text(
            text = "CHAPTERS",
            style = MdCaption,
            color = uber.mute,
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xxs),
        )
        LazyColumn {
            items(visibleChapters(chapters, activeId), key = { it.blockId }) { chapter ->
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
    val indent = spacing.sm + ((chapter.level - 1) * 8).dp
    Text(
        text = chapter.title,
        style = MdBodyMedium.copy(fontSize = 14.sp, lineHeight = 18.sp),
        color = if (active) uber.ink else uber.body,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(rowHover)
            .clickable(onClick = onClick)
            .background(if (hovered) uber.canvasSoft else Color.Transparent)
            .padding(start = indent, end = spacing.sm, top = spacing.xs, bottom = spacing.xs)
            .semantics {
                contentDescription = "Go to ${chapter.title}"
                selected = active
            },
    )
}
