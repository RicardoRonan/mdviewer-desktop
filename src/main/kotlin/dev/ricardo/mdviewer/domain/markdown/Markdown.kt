package dev.ricardo.mdviewer.domain.markdown

sealed interface MdInline {
    data class Text(val text: String) : MdInline
    data class Bold(val children: List<MdInline>) : MdInline
    data class Italic(val children: List<MdInline>) : MdInline
    data class Strike(val children: List<MdInline>) : MdInline
    data class Code(val text: String) : MdInline
    data class Link(val text: String, val url: String) : MdInline
    data class Image(val url: String, val alt: String) : MdInline
}

sealed interface MdBlock {
    val id: String

    data class Heading(override val id: String, val level: Int, val inlines: List<MdInline>) : MdBlock
    data class Paragraph(override val id: String, val inlines: List<MdInline>) : MdBlock
    data class Quote(override val id: String, val inlines: List<MdInline>) : MdBlock
    data class ListItem(
        override val id: String,
        val ordered: Boolean,
        val number: Int,
        val inlines: List<MdInline>,
    ) : MdBlock
    data class Code(override val id: String, val language: String?, val code: String) : MdBlock
    data class Image(override val id: String, val url: String, val alt: String) : MdBlock
    data class Table(override val id: String, val header: List<String>, val rows: List<List<String>>) : MdBlock
    data class Rule(override val id: String) : MdBlock
}

/** Flattened text content of a block, matching exactly what the renderer draws. */
fun inlineText(inlines: List<MdInline>): String = inlines.joinToString("") { inline ->
    when (inline) {
        is MdInline.Text -> inline.text
        is MdInline.Code -> inline.text
        is MdInline.Link -> inline.text
        is MdInline.Image -> inline.alt
        is MdInline.Bold -> inlineText(inline.children)
        is MdInline.Italic -> inlineText(inline.children)
        is MdInline.Strike -> inlineText(inline.children)
    }
}

fun MdBlock.plainText(): String = when (this) {
    is MdBlock.Heading -> inlineText(inlines)
    is MdBlock.Paragraph -> inlineText(inlines)
    is MdBlock.Quote -> inlineText(inlines)
    is MdBlock.ListItem -> inlineText(inlines)
    is MdBlock.Code -> code
    is MdBlock.Image -> alt
    is MdBlock.Table -> tablePlainText(header, rows)
    is MdBlock.Rule -> ""
}

fun MdBlock.isAnnotatable(): Boolean = when (this) {
    is MdBlock.Rule, is MdBlock.Image -> false
    else -> plainText().isNotBlank()
}

/**
 * Canonical plain text for a table: cells joined with tabs inside a row, rows joined with
 * newlines. Annotations over a table index into this string, so the renderer must map each
 * cell's selection back to the same offsets.
 */
fun tablePlainText(header: List<String>, rows: List<List<String>>): String =
    (listOf(header) + rows).joinToString("\n") { row -> row.joinToString("\t") }

/** Offset of the first character of a cell in [tablePlainText]. Row 0 is the header. */
fun tableCellStart(
    header: List<String>,
    rows: List<List<String>>,
    rowIndex: Int,
    columnIndex: Int,
): Int {
    val allRows = listOf(header) + rows
    var offset = 0
    for (r in 0 until rowIndex.coerceIn(0, allRows.size)) {
        offset += allRows[r].sumOf { it.length } + (allRows[r].size - 1).coerceAtLeast(0) + 1
    }
    val row = allRows.getOrElse(rowIndex) { emptyList() }
    for (c in 0 until columnIndex.coerceIn(0, row.size)) {
        offset += row[c].length + 1
    }
    return offset
}
