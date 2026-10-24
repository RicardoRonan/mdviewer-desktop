package dev.ricardo.mdviewer.feature.reader

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import dev.ricardo.mdviewer.ui.components.PillButton
import dev.ricardo.mdviewer.ui.components.PillStyle
import dev.ricardo.mdviewer.ui.theme.LocalSpacing
import dev.ricardo.mdviewer.ui.theme.LocalUberColors
import dev.ricardo.mdviewer.ui.theme.MdCode

@Composable
fun MarkdownEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fontScale: Float,
    modifier: Modifier = Modifier,
) {
    val uber = LocalUberColors.current
    val spacing = LocalSpacing.current
    val editorScroll = rememberScrollState()
    val toolsScroll = rememberScrollState()
    val textStyle = MdCode.merge(
        TextStyle(
            color = uber.ink,
            fontSize = (16 * fontScale).sp,
            lineHeight = (24 * fontScale).sp,
        ),
    )

    Column(modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(toolsScroll)
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            FormatChip("H") { onValueChange(insertMarkdown(value, "# ")) }
            FormatChip("B") { onValueChange(wrapSelection(value, "**", "**")) }
            FormatChip("I") { onValueChange(wrapSelection(value, "*", "*")) }
            FormatChip("List") { onValueChange(insertMarkdown(value, "- ")) }
            FormatChip("Quote") { onValueChange(insertMarkdown(value, "> ")) }
            FormatChip("Code") { onValueChange(wrapSelection(value, "`", "`")) }
            FormatChip("Link") { onValueChange(wrapSelection(value, "[", "](url)")) }
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(uber.ink),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(editorScroll)
                        .padding(horizontal = spacing.xl, vertical = spacing.lg),
                ) {
                    inner()
                }
            },
        )
    }
}

@Composable
private fun FormatChip(label: String, onClick: () -> Unit) {
    PillButton(text = label, onClick = onClick, style = PillStyle.Subtle, compact = true)
}

internal fun wrapSelection(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
    val text = value.text
    val start = value.selection.min.coerceIn(0, text.length)
    val end = value.selection.max.coerceIn(0, text.length)
    val selected = text.substring(start, end).ifEmpty { "text" }
    val inserted = prefix + selected + suffix
    val updated = text.replaceRange(start, end, inserted)
    val cursor = start + inserted.length
    return TextFieldValue(updated, androidx.compose.ui.text.TextRange(cursor))
}

internal fun insertMarkdown(value: TextFieldValue, prefix: String): TextFieldValue {
    val text = value.text
    val start = value.selection.min.coerceIn(0, text.length)
    val end = value.selection.max.coerceIn(0, text.length)
    val selected = text.substring(start, end)
    val inserted = if (selected.isEmpty()) prefix else prefix + selected
    val updated = text.replaceRange(start, end, inserted)
    val cursor = start + inserted.length
    return TextFieldValue(updated, androidx.compose.ui.text.TextRange(cursor))
}
