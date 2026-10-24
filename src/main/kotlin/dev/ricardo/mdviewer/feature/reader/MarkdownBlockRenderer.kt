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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.ricardo.mdviewer.domain.markdown.MdBlock
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
) {
    val spacing = LocalSpacing.current
    val uber = LocalUberColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        blocks.forEach { block ->
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
            )
        }

        is MdBlock.Paragraph -> SelectableMarkdownBlock(
            text = MarkdownStyler.build(block.inlines, highlights, uber, uber.canvasSoft),
            style = MdBodyMedium.scaled(fontScale),
            clearSelection = clearSelection,
            onSelectionChange = onSelectionChange,
            onLinkClick = onLinkClick,
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

        is MdBlock.Table -> TableBlock(block, fontScale, uber)

        is MdBlock.Image -> ImageBlock(block, loadRemoteImages, uber)
    }
}

@Composable
private fun TableBlock(block: MdBlock.Table, fontScale: Float, uber: UberColors) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(uber.canvasSoft),
    ) {
        TableRow(block.header, fontScale, uber, isHeader = true)
        block.rows.forEach { row ->
            HorizontalDivider(color = uber.surfacePressed)
            TableRow(row, fontScale, uber, isHeader = false)
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, fontScale: Float, uber: UberColors, isHeader: Boolean) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        cells.forEach { cell ->
            Text(
                text = cell,
                style = MdBodyMedium.scaled(fontScale).copy(
                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (isHeader) uber.ink else uber.body,
                modifier = Modifier.weight(1f),
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
