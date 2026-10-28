package dev.ricardo.mdviewer.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.ricardo.mdviewer.domain.markdown.MdBlock
import dev.ricardo.mdviewer.domain.markdown.MdInline
import dev.ricardo.mdviewer.domain.markdown.tableCellStart
import dev.ricardo.mdviewer.ui.theme.LocalSpacing
import dev.ricardo.mdviewer.ui.theme.LocalUberColors
import dev.ricardo.mdviewer.ui.theme.MdBodyLarge
import dev.ricardo.mdviewer.ui.theme.MdBodyMedium
import dev.ricardo.mdviewer.ui.theme.MdCaption
import dev.ricardo.mdviewer.ui.theme.MdCode
import dev.ricardo.mdviewer.ui.theme.UberColors

@Composable
fun MarkdownContent(
    blocks: List<MdBlock>,
    highlights: Map<String, List<TextHighlight>>,
    fontScale: Float,
    loadRemoteImages: Boolean,
    clearSelection: Boolean,
    onSelectionChange: (String, IntRange) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBlockPosition: (String, Float) -> Unit = { _, _ -> },
) {
    val spacing = LocalSpacing.current
    val uber = LocalUberColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        blocks.forEach { block ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        onBlockPosition(block.id, coordinates.positionInParent().y)
                    },
            ) {
                RenderBlock(
                    block = block,
                    highlights = highlights[block.id].orEmpty(),
                    fontScale = fontScale,
                    loadRemoteImages = loadRemoteImages,
                    uber = uber,
                    clearSelection = clearSelection,
                    onSelectionChange = { range -> onSelectionChange(block.id, range) },
                    onLinkClick = onLinkClick,
                )
            }
        }
    }
}

@Composable
private fun RenderBlock(
    block: MdBlock,
    highlights: List<TextHighlight>,
    fontScale: Float,
    loadRemoteImages: Boolean,
    uber: UberColors,
    clearSelection: Boolean,
    onSelectionChange: (IntRange) -> Unit,
    onLinkClick: (String) -> Unit,
) {
    val spacing = LocalSpacing.current
    when (block) {
        is MdBlock.Heading -> {
            val style = headingStyle(block.level).scaled(fontScale)
            SelectableMarkdownBlock(
                text = MarkdownStyler.build(block.inlines, highlights, uber, uber.canvasSoft),
                style = style,
                clearSelection = clearSelection,
                onSelectionChange = onSelectionChange,
                onLinkClick = onLinkClick,
                modifier = Modifier.padding(top = if (block.level <= 2) spacing.lg else spacing.sm),
                notes = highlights,
            )
        }

        is MdBlock.Paragraph -> SelectableMarkdownBlock(
            text = MarkdownStyler.build(block.inlines, highlights, uber, uber.canvasSoft),
            style = MdBodyMedium.scaled(fontScale),
            clearSelection = clearSelection,
            onSelectionChange = onSelectionChange,
            onLinkClick = onLinkClick,
            notes = highlights,
        )

        is MdBlock.Quote -> Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(end = spacing.md)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(uber.ink),
            )
            SelectableMarkdownBlock(
                text = MarkdownStyler.build(block.inlines, highlights, uber, uber.canvasSoft),
                style = MdBodyLarge.scaled(fontScale).copy(color = uber.body),
                clearSelection = clearSelection,
                onSelectionChange = onSelectionChange,
                onLinkClick = onLinkClick,
                modifier = Modifier.weight(1f),
                notes = highlights,
            )
        }

        is MdBlock.ListItem -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = if (block.ordered) "${block.number}." else "\u2022",
                style = MdBodyMedium.scaled(fontScale),
                color = uber.body,
                modifier = Modifier.width(28.dp),
            )
            SelectableMarkdownBlock(
                text = MarkdownStyler.build(block.inlines, highlights, uber, uber.canvasSoft),
                style = MdBodyMedium.scaled(fontScale),
                clearSelection = clearSelection,
                onSelectionChange = onSelectionChange,
                onLinkClick = onLinkClick,
                modifier = Modifier.weight(1f),
                notes = highlights,
            )
        }

        is MdBlock.Code -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(uber.canvasSoft)
                .padding(spacing.lg),
        ) {
            if (!block.language.isNullOrBlank()) {
                Text(text = block.language!!.uppercase(), style = MdCaption, color = uber.mute)
            }
            Text(
                text = block.code,
                style = MdCode.scaled(fontScale),
                color = uber.ink,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            )
        }

        is MdBlock.Rule -> HorizontalDivider(
            color = uber.surfacePressed,
            modifier = Modifier.padding(vertical = spacing.sm),
        )

        is MdBlock.Table -> TableBlock(
            block = block,
            highlights = highlights,
            fontScale = fontScale,
            clearSelection = clearSelection,
            onSelectionChange = onSelectionChange,
            onLinkClick = onLinkClick,
            uber = uber,
        )

        is MdBlock.Image -> ImageBlock(block, loadRemoteImages, uber)
    }
}

@Composable
private fun TableBlock(
    block: MdBlock.Table,
    highlights: List<TextHighlight>,
    fontScale: Float,
    clearSelection: Boolean,
    onSelectionChange: (IntRange) -> Unit,
    onLinkClick: (String) -> Unit,
    uber: UberColors,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(uber.canvasSoft),
    ) {
        TableRow(
            table = block,
            cells = block.header,
            rowIndex = 0,
            isHeader = true,
            highlights = highlights,
            fontScale = fontScale,
            clearSelection = clearSelection,
            onSelectionChange = onSelectionChange,
            onLinkClick = onLinkClick,
            uber = uber,
        )
        block.rows.forEachIndexed { index, row ->
            HorizontalDivider(color = uber.surfacePressed)
            TableRow(
                table = block,
                cells = row,
                rowIndex = index + 1,
                isHeader = false,
                highlights = highlights,
                fontScale = fontScale,
                clearSelection = clearSelection,
                onSelectionChange = onSelectionChange,
                onLinkClick = onLinkClick,
                uber = uber,
            )
        }
    }
}

@Composable
private fun TableRow(
    table: MdBlock.Table,
    cells: List<String>,
    rowIndex: Int,
    isHeader: Boolean,
    highlights: List<TextHighlight>,
    fontScale: Float,
    clearSelection: Boolean,
    onSelectionChange: (IntRange) -> Unit,
    onLinkClick: (String) -> Unit,
    uber: UberColors,
) {
    val spacing = LocalSpacing.current
    val textColor = if (isHeader) uber.ink else uber.body
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        cells.forEachIndexed { columnIndex, cell ->
            val cellStart = tableCellStart(table.header, table.rows, rowIndex, columnIndex)
            val cellEnd = cellStart + cell.length
            val cellHighlights = highlights.mapNotNull { highlight ->
                val from = maxOf(highlight.start, cellStart)
                val to = minOf(highlight.end, cellEnd)
                if (to <= from) null else TextHighlight(from - cellStart, to - cellStart, highlight.color)
            }
            SelectableMarkdownBlock(
                text = MarkdownStyler.build(
                    inlines = listOf(MdInline.Text(cell)),
                    highlights = cellHighlights,
                    uber = uber,
                    codeBackground = uber.canvasSoft,
                    textColor = textColor,
                ),
                style = MdBodyMedium.scaled(fontScale).copy(
                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                ),
                clearSelection = clearSelection,
                onSelectionChange = onSelectionChange,
                onLinkClick = onLinkClick,
                modifier = Modifier.weight(1f),
                selectionOffset = cellStart,
                notes = cellHighlights,
            )
        }
    }
}

@Composable
private fun ImageBlock(block: MdBlock.Image, loadRemoteImages: Boolean, uber: UberColors) {
    val spacing = LocalSpacing.current
    val isRemote = block.url.startsWith("http://") || block.url.startsWith("https://")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(uber.canvasSoft)
            .padding(spacing.lg),
    ) {
        if (isRemote && loadRemoteImages) {
            AsyncImage(
                model = block.url,
                contentDescription = block.alt,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(text = block.alt.ifBlank { "Image" }, style = MdBodyMedium.scaled(1f), color = uber.ink)
                Text(
                    text = if (isRemote) "Remote image hidden. Enable in Settings." else block.url,
                    style = MdCaption,
                    color = uber.mute,
                )
            }
        }
    }
}

private fun headingStyle(level: Int): TextStyle = when (level) {
    1 -> TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
    2 -> TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    3 -> TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
    4 -> TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
    5 -> TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
    else -> TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
}

private fun TextStyle.scaled(scale: Float): TextStyle = copy(
    fontSize = fontSize * scale,
    lineHeight = lineHeight * scale,
)
