package dev.ricardo.mdviewer.domain.markdown

/**
 * A small, dependency-free CommonMark-subset parser. It renders headings, paragraphs, blockquotes,
 * lists, task lists, fenced code, tables, images, and horizontal rules.
 *
 * The parser is intentionally owned by the app (rather than a Compose markdown library) because the
 * annotation engine needs exact plain-text offsets per block, which generic renderers do not expose.
 */
object MarkdownParser {

    private val headingRegex = Regex("^(#{1,6})\\s+(.*?)\\s*#*\\s*$")
    private val hrRegex = Regex("^\\s{0,3}([-*_])(\\s*\\1){2,}\\s*$")
    private val bulletRegex = Regex("^(\\s*)([-*+])\\s+(.*)$")
    private val orderedRegex = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
    private val tableSeparatorRegex = Regex("^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")
    private val setextRegex = Regex("^\\s{0,3}(=+|-+)\\s*$")

    fun parse(source: String): List<MdBlock> {
        val lines = source.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val blocks = mutableListOf<MdBlock>()
        var counter = 0
        fun id() = "b${counter++}"

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank()) {
                i++
                continue
            }

            val fence = fenceMarker(line)
            if (fence != null) {
                val lang = line.trimStart().removePrefix(fence).trim().ifBlank { null }
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith(fence)) {
                    if (code.isNotEmpty()) code.append('\n')
                    code.append(lines[i])
                    i++
                }
                if (i < lines.size) i++ // consume closing fence
                blocks.add(MdBlock.Code(id(), lang, code.toString()))
                continue
            }

            val heading = headingRegex.matchEntire(line)
            if (heading != null) {
                val level = heading.groupValues[1].length
                blocks.add(MdBlock.Heading(id(), level, parseInlines(heading.groupValues[2])))
                i++
                continue
            }

            if (hrRegex.matches(line)) {
                blocks.add(MdBlock.Rule(id()))
                i++
                continue
            }

            if (looksLikeTable(lines, i)) {
                val table = parseTable(lines, i)
                blocks.add(MdBlock.Table(id(), table.first, table.second))
                i = table.third
                continue
            }

            if (line.trimStart().startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                    quoteLines.add(lines[i].trimStart().removePrefix(">").trimStart())
                    i++
                }
                blocks.add(MdBlock.Quote(id(), parseInlines(quoteLines.joinToString(" "))))
                continue
            }

            val bullet = bulletRegex.matchEntire(line)
            if (bullet != null) {
                blocks.add(MdBlock.ListItem(id(), ordered = false, number = 0, inlines = listInlines(bullet.groupValues[3])))
                i++
                continue
            }

            val ordered = orderedRegex.matchEntire(line)
            if (ordered != null) {
                blocks.add(
                    MdBlock.ListItem(
                        id = id(),
                        ordered = true,
                        number = ordered.groupValues[2].toIntOrNull() ?: 1,
                        inlines = listInlines(ordered.groupValues[3]),
                    ),
                )
                i++
                continue
            }

            // Paragraph: gather until a blank line or the start of another block.
            val paragraph = mutableListOf<String>()
            while (i < lines.size && lines[i].isNotBlank() && !isBlockStart(lines, i)) {
                paragraph.add(lines[i].trim())
                i++
            }
            if (paragraph.isEmpty()) {
                paragraph.add(line.trim())
                i++
            }
            blocks.add(MdBlock.Paragraph(id(), parseInlines(paragraph.joinToString(" "))))
        }
        return blocks
    }

    private fun listInlines(content: String): List<MdInline> {
        val checkbox = Regex("^\\[([ xX])\\]\\s+(.*)$").matchEntire(content)
        return if (checkbox != null) {
            val mark = if (checkbox.groupValues[1].isBlank()) "\u2610 " else "\u2611 "
            listOf(MdInline.Text(mark)) + parseInlines(checkbox.groupValues[2])
        } else {
            parseInlines(content)
        }
    }

    private fun fenceMarker(line: String): String? {
        val t = line.trimStart()
        return when {
            t.startsWith("```") -> "```"
            t.startsWith("~~~") -> "~~~"
            else -> null
        }
    }

    private fun isBlockStart(lines: List<String>, index: Int): Boolean {
        val line = lines[index]
        if (line.isBlank()) return true
        if (fenceMarker(line) != null) return true
        if (headingRegex.matchEntire(line) != null) return true
        if (hrRegex.matches(line)) return true
        if (line.trimStart().startsWith(">")) return true
        if (bulletRegex.matchEntire(line) != null) return true
        if (orderedRegex.matchEntire(line) != null) return true
        if (looksLikeTable(lines, index)) return true
        return false
    }

    private fun looksLikeTable(lines: List<String>, index: Int): Boolean {
        if (index + 1 >= lines.size) return false
        val header = lines[index]
        val separator = lines[index + 1]
        if (!header.contains('|')) return false
        if (setextRegex.matches(separator)) return false
        return tableSeparatorRegex.matches(separator) && separator.contains('-')
    }

    /** Returns header, rows, and the next line index. */
    private fun parseTable(lines: List<String>, index: Int): Triple<List<String>, List<List<String>>, Int> {
        val header = splitRow(lines[index])
        var i = index + 2
        val rows = mutableListOf<List<String>>()
        while (i < lines.size && lines[i].isNotBlank() && lines[i].contains('|')) {
            rows.add(splitRow(lines[i]))
            i++
        }
        return Triple(header, rows, i)
    }

    private fun splitRow(line: String): List<String> {
        val trimmed = line.trim().removePrefix("|").removeSuffix("|")
        return trimmed.split('|').map { it.trim() }
    }

    fun parseInlines(source: String): List<MdInline> {
        val out = mutableListOf<MdInline>()
        val buffer = StringBuilder()
        fun flush() {
            if (buffer.isNotEmpty()) {
                out.add(MdInline.Text(buffer.toString()))
                buffer.clear()
            }
        }

        var i = 0
        while (i < source.length) {
            val c = source[i]
            when {
                c == '\\' && i + 1 < source.length -> {
                    buffer.append(source[i + 1]); i += 2
                }
                c == '`' -> {
                    val close = source.indexOf('`', i + 1)
                    if (close > i) {
                        flush(); out.add(MdInline.Code(source.substring(i + 1, close))); i = close + 1
                    } else {
                        buffer.append(c); i++
                    }
                }
                (c == '*' || c == '_') && i + 1 < source.length && source[i + 1] == c -> {
                    val marker = "" + c + c
                    val close = source.indexOf(marker, i + 2)
                    if (close > i) {
                        flush(); out.add(MdInline.Bold(parseInlines(source.substring(i + 2, close)))); i = close + 2
                    } else {
                        buffer.append(c); i++
                    }
                }
                c == '~' && i + 1 < source.length && source[i + 1] == '~' -> {
                    val close = source.indexOf("~~", i + 2)
                    if (close > i) {
                        flush(); out.add(MdInline.Strike(parseInlines(source.substring(i + 2, close)))); i = close + 2
                    } else {
                        buffer.append(c); i++
                    }
                }
                c == '*' || c == '_' -> {
                    val close = source.indexOf(c, i + 1)
                    if (close > i) {
                        flush(); out.add(MdInline.Italic(parseInlines(source.substring(i + 1, close)))); i = close + 1
                    } else {
                        buffer.append(c); i++
                    }
                }
                c == '!' && i + 1 < source.length && source[i + 1] == '[' -> {
                    val parsed = parseBracket(source, i + 1)
                    if (parsed != null) {
                        flush(); out.add(MdInline.Image(parsed.third, parsed.first)); i = parsed.second + 1
                    } else {
                        buffer.append(c); i++
                    }
                }
                c == '[' -> {
                    val parsed = parseBracket(source, i)
                    if (parsed != null) {
                        flush(); out.add(MdInline.Link(parsed.first, parsed.third)); i = parsed.second + 1
                    } else {
                        buffer.append(c); i++
                    }
                }
                c == '<' -> {
                    val close = source.indexOf('>', i + 1)
                    val inner = if (close > i) source.substring(i + 1, close) else ""
                    if (inner.startsWith("http://") || inner.startsWith("https://")) {
                        flush(); out.add(MdInline.Link(inner, inner)); i = close + 1
                    } else {
                        buffer.append(c); i++
                    }
                }
                source.startsWith("https://", i) || source.startsWith("http://", i) -> {
                    var end = i
                    while (end < source.length && !source[end].isWhitespace() && source[end] != '<' && source[end] != '>') {
                        end++
                    }
                    while (end > i && source[end - 1] in ".,;:!?\"')") end--
                    val url = source.substring(i, end)
                    if (url.length > "https://".length) {
                        flush(); out.add(MdInline.Link(url, url)); i = end
                    } else {
                        buffer.append(c); i++
                    }
                }
                else -> {
                    buffer.append(c); i++
                }
            }
        }
        flush()
        return out
    }

    /** Parses `[label](target)` starting at [open]. Returns label, index of `)`, target. */
    private fun parseBracket(source: String, open: Int): Triple<String, Int, String>? {
        val closeLabel = source.indexOf(']', open + 1)
        if (closeLabel < 0 || closeLabel + 1 >= source.length || source[closeLabel + 1] != '(') return null
        val closeTarget = source.indexOf(')', closeLabel + 2)
        if (closeTarget < 0) return null
        val label = source.substring(open + 1, closeLabel)
        val target = source.substring(closeLabel + 2, closeTarget).trim().substringBefore(' ')
        return Triple(label, closeTarget, target)
    }
}
