package dev.ricardo.mdviewer.feature.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

/**
 * A read-only, selectable markdown text block. Selection offsets are reported upward; the reader
 * presents the action bar (Highlight / Note / Copy) so it stays reliably tappable.
 *
 * Collapsed selections are only reported when the reader taps inside this block. A text field
 * collapses its own selection when it loses focus, and reporting that would tear down the action
 * bar the moment the user presses one of its buttons.
 *
 * [selectionOffset] shifts reported offsets so callers can compose a selection out of several
 * fields (table cells) that share one block id and one plain-text offset space.
 *
 * [notes] are this block's annotated ranges that carry note text. Hovering one shows its note in a
 * small card. Plain highlights stay silent, and note cards stay out of the way while the pointer is
 * pressed or a selection is live, so drag-selection never fights the card.
 */
@Suppress("DEPRECATION")
@Composable
fun SelectableMarkdownBlock(
    text: AnnotatedString,
    style: TextStyle,
    clearSelection: Boolean,
    onSelectionChange: (IntRange) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectionOffset: Int = 0,
    notes: List<TextHighlight> = emptyList(),
) {
    var value by remember { mutableStateOf(TextFieldValue(text)) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val suppression = remember { SuppressSystemTextToolbar() }
    val gapPx = with(LocalDensity.current) { 6.dp.roundToPx() }

    var hoveredNote by remember { mutableStateOf<TextHighlight?>(null) }
    var hoverAnchor by remember { mutableStateOf(Rect.Zero) }
    var showNote by remember { mutableStateOf(false) }

    LaunchedEffect(text) {
        if (value.annotatedString != text) {
            value = TextFieldValue(text, value.selection, value.composition)
        }
    }

    LaunchedEffect(value.selection, selectionOffset) {
        val selection = value.selection
        if (selection.collapsed) return@LaunchedEffect
        onSelectionChange(
            (selection.min + selectionOffset) until (selection.max + selectionOffset),
        )
    }

    LaunchedEffect(clearSelection) {
        if (clearSelection && !value.selection.collapsed) {
            value = value.copy(selection = TextRange.Zero)
        }
    }

    // Hover-to-preview is deliberately delayed: at ~320 ms a note never flashes while the pointer
    // crosses a document, but it still feels immediate when the reader stops on a highlight.
    LaunchedEffect(hoveredNote, value.selection) {
        val note = hoveredNote
        if (note == null || !value.selection.collapsed) {
            showNote = false
            return@LaunchedEffect
        }
        delay(320)
        showNote = true
    }

    Box(modifier) {
        CompositionLocalProvider(LocalTextToolbar provides suppression) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                readOnly = true,
                textStyle = style,
                onTextLayout = { layout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(text, notes) {
                        if (notes.none { it.note.isNotBlank() }) return@pointerInput
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Exit) {
                                    if (hoveredNote != null) hoveredNote = null
                                    continue
                                }
                                if (event.changes.any { it.pressed }) {
                                    if (hoveredNote != null) hoveredNote = null
                                    continue
                                }
                                if (event.type != PointerEventType.Move) continue
                                val position = event.changes.firstOrNull()?.position ?: continue
                                val result = layout ?: continue
                                if (text.isEmpty()) continue
                                val offset = result
                                    .getOffsetForPosition(position)
                                    .coerceIn(0, text.length - 1)
                                val note = noteUnder(offset, notes)
                                if (note == null) {
                                    if (hoveredNote != null) hoveredNote = null
                                    continue
                                }
                                if (note != hoveredNote) {
                                    hoveredNote = note
                                    val box = result.getBoundingBox(offset)
                                    hoverAnchor = Rect(box.left, box.top, box.right, box.bottom)
                                }
                            }
                        }
                    }
                    .pointerInput(text) {
                        val slop = viewConfiguration.touchSlop
                        val longPress = viewConfiguration.longPressTimeoutMillis
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                            )
                            val origin = down.position
                            val pointer = down.id
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == pointer } ?: break
                                if (change.pressed) continue
                                val tap = (change.position - origin).getDistance() <= slop &&
                                    change.uptimeMillis - down.uptimeMillis < longPress
                                if (tap) {
                                    val result = layout
                                    val offset = result?.getOffsetForPosition(change.position)
                                    val url = if (offset != null) {
                                        text.getStringAnnotations(
                                            LINK_TAG,
                                            offset,
                                            (offset + 1).coerceAtMost(text.length),
                                        ).firstOrNull()?.item
                                    } else {
                                        null
                                    }
                                    if (!url.isNullOrBlank()) {
                                        onLinkClick(url)
                                    } else {
                                        // A tap without a drag is the reader dismissing the selection.
                                        onSelectionChange(IntRange.EMPTY)
                                    }
                                }
                                break
                            }
                        }
                    },
            )
        }

        val note = hoveredNote
        if (showNote && note != null && value.selection.collapsed) {
            Popup(
                popupPositionProvider = NoteTooltipPositionProvider(hoverAnchor, gapPx),
                properties = PopupProperties(focusable = false, dismissOnClickOutside = false),
            ) {
                NoteTooltip(note = note.note, tags = note.tags)
            }
        }
    }
}
