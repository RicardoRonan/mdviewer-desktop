package dev.ricardo.mdviewer.domain.anchor

import dev.ricardo.mdviewer.domain.model.QuoteSelector

/**
 * Layered anchoring in the spirit of the W3C Web Annotation model. Annotations are scoped to a
 * single rendered block, so offsets index into [blockText] exactly as rendered.
 *
 * Resolution order: exact + context match -> prefix-assisted match -> normalized retry -> orphan.
 */
object AnchorEngine {

    const val CONTEXT_LENGTH = 32

    fun buildSelector(blockId: String, blockText: String, start: Int, end: Int): QuoteSelector {
        val safeStart = start.coerceIn(0, blockText.length)
        val safeEnd = end.coerceIn(safeStart, blockText.length)
        val prefixStart = (safeStart - CONTEXT_LENGTH).coerceAtLeast(0)
        val suffixEnd = (safeEnd + CONTEXT_LENGTH).coerceAtMost(blockText.length)
        return QuoteSelector(
            exact = blockText.substring(safeStart, safeEnd),
            prefix = blockText.substring(prefixStart, safeStart),
            suffix = blockText.substring(safeEnd, suffixEnd),
            start = safeStart,
            end = safeEnd,
            blockId = blockId,
        )
    }

    /**
     * Returns the resolved character range within [blockText], or null if the quote can no longer
     * be found (the annotation is then surfaced as orphaned).
     */
    fun resolve(blockText: String, selector: QuoteSelector): IntRange? {
        val exact = selector.exact
        if (exact.isEmpty() || blockText.isEmpty()) return null

        exactMatch(blockText, exact, selector.prefix, selector.suffix, selector.start)?.let { return it }
        prefixAssisted(blockText, exact, selector.prefix)?.let { return it }
        normalizedExact(blockText, exact)?.let { return it }
        return null
    }

    private fun exactMatch(
        text: String,
        exact: String,
        prefix: String,
        suffix: String,
        hint: Int,
    ): IntRange? {
        var index = text.indexOf(exact)
        if (index < 0) return null
        var best: Int? = null
        var bestScore = -1
        while (index >= 0) {
            val prefixScore = commonSuffixLength(text.substring(0, index), prefix)
            val suffixScore = commonPrefixLength(text.substring(index + exact.length), suffix)
            val distancePenalty = kotlin.math.abs(index - hint)
            val score = prefixScore + suffixScore - distancePenalty.coerceAtMost(64)
            if (score > bestScore) {
                bestScore = score
                best = index
            }
            index = text.indexOf(exact, index + 1)
        }
        val start = best ?: return null
        return start until (start + exact.length)
    }

    private fun prefixAssisted(text: String, exact: String, prefix: String): IntRange? {
        if (prefix.isEmpty()) return null
        val candidates = listOf(prefix, prefix.takeLast(12))
        for (p in candidates) {
            if (p.isEmpty()) continue
            val pIndex = text.lastIndexOf(p)
            if (pIndex >= 0) {
                val start = pIndex + p.length
                if (text.regionMatches(start, exact, 0, exact.length, ignoreCase = true)) {
                    return start until (start + exact.length)
                }
            }
        }
        return null
    }

    private fun normalizedExact(text: String, exact: String): IntRange? {
        val normalizedExact = exact.trim()
        if (normalizedExact.isEmpty()) return null
        val idx = text.indexOf(normalizedExact)
        return if (idx >= 0) idx until (idx + normalizedExact.length) else null
    }

    private fun commonPrefixLength(a: String, b: String): Int {
        val max = minOf(a.length, b.length)
        var i = 0
        while (i < max && a[i].equals(b[i], ignoreCase = true)) i++
        return i
    }

    private fun commonSuffixLength(a: String, b: String): Int {
        val max = minOf(a.length, b.length)
        var i = 0
        while (i < max && a[a.length - 1 - i].equals(b[b.length - 1 - i], ignoreCase = true)) i++
        return i
    }
}
