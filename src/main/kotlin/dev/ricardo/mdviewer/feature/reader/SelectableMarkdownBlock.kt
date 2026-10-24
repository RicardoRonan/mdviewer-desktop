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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue

/**
 * A read-only, selectable markdown text block. Selection offsets are reported upward; the reader
 * presents the action bar (Highlight / Note / Copy) so it stays reliably tappable.
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
) {
    var value by remember { mutableStateOf(TextFieldValue(text)) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val suppression = remember { SuppressSystemTextToolbar() }

    LaunchedEffect(text) {
        if (value.annotatedString != text) {
            value = TextFieldValue(text, value.selection, value.composition)
        }
    }

    LaunchedEffect(value.selection) {
        val selection = value.selection
        onSelectionChange(
            if (selection.collapsed) IntRange.EMPTY else selection.min until selection.max,
        )
    }

    LaunchedEffect(clearSelection) {
        if (clearSelection && !value.selection.collapsed) {
            value = value.copy(selection = TextRange.Zero)
        }
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
                                    val result = layout ?: break
                                    val offset = result.getOffsetForPosition(change.position)
                                    val url = text.getStringAnnotations(
                                        LINK_TAG,
                                        offset,
                                        (offset + 1).coerceAtMost(text.length),
                                    ).firstOrNull()?.item
                                    if (!url.isNullOrBlank()) onLinkClick(url)
                                }
                                break
                            }
                        }
                    },
            )
        }
    }
}
