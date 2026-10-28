package dev.ricardo.mdviewer.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import dev.ricardo.mdviewer.ui.theme.LocalUberColors
import dev.ricardo.mdviewer.ui.theme.MdBodyMedium
import dev.ricardo.mdviewer.ui.theme.MdCaption
import kotlin.math.roundToInt

/**
 * Compact note card shown over a highlighted range on hover.
 *
 * Material 3's tooltip cannot carry this card: `PlainTooltip` is an extension on `TooltipScope`,
 * which only `TooltipBox` provides, and `TooltipBox` anchors to a whole layout node rather than a
 * character range. So the card stays ours, but it adopts Material's tooltip contract: announced
 * through a polite live region, shown after a hover delay, dismissed by Escape and mouse-out.
 */
@Composable
fun NoteTooltip(
    note: String,
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    val uber = LocalUberColors.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .widthIn(max = 320.dp)
            .clip(shape)
            .background(uber.canvas)
            .border(1.dp, uber.hairlineMid.copy(alpha = 0.5f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = note,
            style = MdBodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = uber.ink,
            maxLines = 12,
            overflow = TextOverflow.Ellipsis,
        )
        if (tags.isNotEmpty()) {
            Text(
                text = tags.joinToString(" ") { "#$it" },
                style = MdCaption,
                fontWeight = FontWeight.Medium,
                color = uber.mute,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Places a tooltip above [anchor] (a rectangle in the text field's local space) and flips it
 * below when there is no room, keeping it inside the window.
 */
class NoteTooltipPositionProvider(
    private val anchor: Rect,
    private val gapPx: Int,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - gapPx).coerceAtLeast(gapPx)
        val x = (anchorBounds.left + anchor.center.x.roundToInt() - popupContentSize.width / 2)
            .coerceIn(gapPx, maxX)

        val above = anchorBounds.top + anchor.top.roundToInt() - popupContentSize.height - gapPx
        val below = anchorBounds.top + anchor.bottom.roundToInt() + gapPx
        val maxY = (windowSize.height - popupContentSize.height - gapPx).coerceAtLeast(gapPx)
        val y = if (above >= gapPx) above else below.coerceIn(gapPx, maxY)

        return IntOffset(x, y)
    }
}

/** The first note whose range contains [offset]. */
fun noteUnder(offset: Int, notes: List<TextHighlight>): TextHighlight? =
    notes.firstOrNull { highlight ->
        highlight.note.isNotBlank() && offset >= highlight.start && offset < highlight.end
    }
