package dev.ricardo.mdviewer.feature.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import dev.ricardo.mdviewer.domain.markdown.MdInline
import dev.ricardo.mdviewer.ui.theme.UberColors

data class TextHighlight(
    val start: Int,
    val end: Int,
    val color: Color,
    val note: String = "",
    val tags: List<String> = emptyList(),
)

const val LINK_TAG = "URL"

/** Renders an inline markdown tree to an [AnnotatedString], applying annotation highlights last. */
object MarkdownStyler {

    fun build(
        inlines: List<MdInline>,
        highlights: List<TextHighlight>,
        uber: UberColors,
        codeBackground: Color,
        textColor: Color = uber.ink,
    ): AnnotatedString {
        val builder = AnnotatedString.Builder()
        appendInlines(builder, inlines, SpanStyle(color = textColor), uber, codeBackground)
        for (highlight in highlights) {
            val start = highlight.start.coerceIn(0, builder.length)
            val end = highlight.end.coerceIn(0, builder.length)
            if (end > start) {
                builder.addStyle(SpanStyle(background = highlight.color), start, end)
            }
        }
        return builder.toAnnotatedString()
    }

    private fun appendInlines(
        builder: AnnotatedString.Builder,
        inlines: List<MdInline>,
        style: SpanStyle,
        uber: UberColors,
        codeBackground: Color,
    ) {
        for (inline in inlines) {
            when (inline) {
                is MdInline.Text -> builder.withStyle(style) { append(inline.text) }

                is MdInline.Code -> builder.withStyle(
                    style.merge(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)),
                ) { append(inline.text) }

                is MdInline.Bold -> appendInlines(
                    builder,
                    inline.children,
                    style.merge(SpanStyle(fontWeight = FontWeight.Bold)),
                    uber,
                    codeBackground,
                )

                is MdInline.Italic -> appendInlines(
                    builder,
                    inline.children,
                    style.merge(SpanStyle(fontStyle = FontStyle.Italic)),
                    uber,
                    codeBackground,
                )

                is MdInline.Strike -> appendInlines(
                    builder,
                    inline.children,
                    style.merge(SpanStyle(textDecoration = TextDecoration.LineThrough)),
                    uber,
                    codeBackground,
                )

                is MdInline.Link -> {
                    builder.pushStringAnnotation(LINK_TAG, inline.url)
                    builder.withStyle(
                        style.merge(
                            SpanStyle(
                                color = uber.link,
                                textDecoration = TextDecoration.Underline,
                                background = uber.link.copy(alpha = 0.16f),
                            ),
                        ),
                    ) { append(inline.text) }
                    builder.pop()
                }

                is MdInline.Image -> builder.withStyle(
                    style.merge(SpanStyle(fontStyle = FontStyle.Italic, color = uber.body)),
                ) { append(inline.alt.ifBlank { "image" }) }
            }
        }
    }
}
