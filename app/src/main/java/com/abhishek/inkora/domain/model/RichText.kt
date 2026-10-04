package com.abhishek.inkora.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Real rich-text model for Inkora V1.
 *
 * Formatting is stored as span/block structure, NEVER as visible syntax.
 * The user never sees `**`, `_`, `<u>`, `~~`, `[ ]` markers.
 *
 * Persistence: [Note.content] holds [Json]-encoded [RichContent] when
 * [Note.contentFormat] == `"rich-v1"`. Older notes (`"md-v1"`/plain) are
 * migrated on open via [migrate] — original text is preserved, markers are
 * converted to spans where safe.
 */
enum class SpanKind { BOLD, ITALIC, UNDERLINE, STRIKE }

enum class BlockKind { PARAGRAPH, BULLET, NUMBERED, CHECK, HEADING1, HEADING2, HEADING3, QUOTE, DIVIDER }

enum class ParaAlign { LEFT, CENTER, RIGHT, JUSTIFY }

@Serializable
data class RichSpan(val start: Int, val end: Int, val kind: SpanKind)

@Serializable
data class RichBlock(val line: Int, val kind: BlockKind, val checked: Boolean = false, val indent: Int = 0)

@Serializable
data class RichAlign(val line: Int, val align: ParaAlign)

@Serializable
data class RichContent(
    val text: String = "",
    val spans: List<RichSpan> = emptyList(),
    val blocks: List<RichBlock> = emptyList(),
    val aligns: List<RichAlign> = emptyList()
)

private val RichJson = Json { ignoreUnknownKeys = true }

object RichText {
    const val FORMAT = "rich-v1"

    fun empty(): RichContent = RichContent()

    fun encode(c: RichContent): String = RichJson.encodeToString(RichContent.serializer(), c)

    fun decodeOrNull(raw: String): RichContent? = runCatching {
        RichJson.decodeFromString(RichContent.serializer(), raw)
    }.getOrNull()

    // ---------- span ops (selection-scoped, never whole-document) ----------

    /** Toggle [kind] over [start,end). Collapsed selections are a no-op (caller tracks pending attrs). */
    fun toggleSpan(c: RichContent, start: Int, end: Int, kind: SpanKind): RichContent {
        val s = start.coerceIn(0, c.text.length)
        val e = end.coerceIn(0, c.text.length)
        if (s >= e) return c
        val fullyCovered = (s until e).all { i -> c.spans.any { it.kind == kind && it.start <= i && i < it.end } }
        val kept = c.spans.filterNot { it.kind == kind && it.end > s && it.start < e }.toMutableList()
        if (fullyCovered) {
            // Remove kind from range, preserving coverage outside it (split spans).
            c.spans.filter { it.kind == kind && it.end > s && it.start < e }.forEach { sp ->
                if (sp.start < s) kept.add(RichSpan(sp.start, s, kind))
                if (sp.end > e) kept.add(RichSpan(e, sp.end, kind))
            }
            return c.copy(spans = kept.sortedWith(compareBy({ it.start }, { it.end })))
        }
        var ns = s
        var ne = e
        val overlapping = c.spans.filter { it.kind == kind && it.end >= s && it.start <= e }
        overlapping.forEach {
            ns = minOf(ns, it.start.coerceAtLeast(0))
            ne = maxOf(ne, it.end.coerceAtMost(c.text.length))
        }
        kept.removeAll(overlapping.toSet())
        kept.add(RichSpan(ns, ne, kind))
        return c.copy(spans = kept.sortedWith(compareBy({ it.start }, { it.end })))
    }

    fun spansIn(c: RichContent, start: Int, end: Int): List<RichSpan> =
        c.spans.filter { it.end > start && it.start < end }

    // ---------- block ops (line-scoped) ----------

    fun linesOf(text: String): List<String> = text.split('\n')

    fun lineIndexAt(text: String, offset: Int): Int {
        val o = offset.coerceIn(0, text.length)
        return text.substring(0, o).count { it == '\n' }
    }

    fun lineRange(text: String, line: Int): IntRange {
        val lines = linesOf(text)
        var pos = 0
        lines.forEachIndexed { i, l ->
            if (i == line) return pos until pos + l.length
            pos += l.length + 1
        }
        return text.length until text.length
    }

    fun linesInSelection(text: String, start: Int, end: Int): IntRange {
        val s = start.coerceIn(0, text.length)
        val e = end.coerceIn(0, text.length)
        if (s >= e) return lineIndexAt(text, s)..lineIndexAt(text, s)
        // A selection ending exactly at a line start belongs to the previous line.
        val last = if (e > 0 && text[e - 1] == '\n') (e - 1) else e
        return lineIndexAt(text, s)..lineIndexAt(text, last.coerceAtLeast(s))
    }

    fun blockAt(c: RichContent, line: Int): RichBlock =
        c.blocks.firstOrNull { it.line == line } ?: RichBlock(line, BlockKind.PARAGRAPH)

    /**
     * Toggle [kind] over [lineRange]. If every line already has [kind] they all
     * revert to PARAGRAPH; otherwise every line becomes [kind]. Text is untouched.
     */
    fun setBlock(c: RichContent, lineRange: IntRange, kind: BlockKind): RichContent {
        val allSet = lineRange.all { blockAt(c, it).kind == kind }
        val others = c.blocks.filter { it.line !in lineRange }.toMutableList()
        if (!allSet) {
            lineRange.forEach { others.add(RichBlock(it, kind, checked = false)) }
        }
        return c.copy(blocks = others.sortedBy { it.line }.map { it.copy(line = it.line) })
    }

    fun toggleCheck(c: RichContent, line: Int): RichContent {
        val cur = blockAt(c, line)
        val others = c.blocks.filter { it.line != line }.toMutableList()
        return if (cur.kind == BlockKind.CHECK) {
            others.add(RichBlock(line, BlockKind.CHECK, checked = !cur.checked))
            c.copy(blocks = others.sortedBy { it.line })
        } else {
            c
        }
    }

    /**
     * Enter key on [line]: empty list item exits to PARAGRAPH; list kinds
     * continue; headings/quotes/dividers always break to PARAGRAPH.
     * Returns the block list for text with [newLineCount] lines after the split.
     */
    fun enterBlocks(c: RichContent, line: Int, lineTextIsBlank: Boolean, newLineCount: Int): List<RichBlock> {
        val cur = blockAt(c, line).kind
        if (cur == BlockKind.PARAGRAPH || cur == BlockKind.DIVIDER) {
            return c.blocks.filter { it.line != line || cur == BlockKind.PARAGRAPH }
                .map { if (it.line > line) it.copy(line = it.line + 1) else it }
                .sortedBy { it.line }
        }
        if (cur == BlockKind.HEADING1 || cur == BlockKind.HEADING2 ||
            cur == BlockKind.HEADING3 || cur == BlockKind.QUOTE
        ) {
            // Structural one-liners never continue: new line is a paragraph.
            return c.blocks
                .map { if (it.line > line) it.copy(line = it.line + 1) else it }
                .sortedBy { it.line }
        }
        if (lineTextIsBlank) {
            return c.blocks.filter { it.line != line }
                .map { if (it.line > line) it.copy(line = it.line + 1) else it }
                .sortedBy { it.line }
        }
        return c.blocks.flatMap {
            if (it.line == line) listOf(it, RichBlock(line + 1, cur, checked = false))
            else if (it.line > line) listOf(it.copy(line = it.line + 1))
            else listOf(it)
        }.take(newLineCount).sortedBy { it.line }
    }

    // ---------- plain / preview text (no syntax, ever) ----------

    /**
     * Display title: the user's title, or the first meaningful content line when
     * the title is blank. Never overwrites the stored title — presentation only.
     */
    fun displayTitle(title: String, rawContent: String, format: String): String {
        if (title.isNotBlank()) return title
        val clean = if (format == FORMAT) {
            decodeOrNull(rawContent)?.text ?: stripSyntax(rawContent)
        } else stripSyntax(rawContent)
        return clean.lines().firstOrNull { it.isNotBlank() }?.trim()?.take(80) ?: ""
    }

    /** Clean human-readable text with real glyphs (•, 1., ☐/☑) — no markers. */
    fun plain(c: RichContent): String {
        val lines = linesOf(c.text)
        var n = 0
        return lines.mapIndexed { i, l ->
            val b = blockAt(c, i)
            val indented = "  ".repeat(b.indent.coerceIn(0, 4)) + l
            when (b.kind) {
                BlockKind.BULLET -> "• $indented"
                BlockKind.NUMBERED -> { n += 1; "$n. $indented" }
                BlockKind.CHECK -> (if (b.checked) "☑ " else "☐ ") + indented
                BlockKind.QUOTE -> "“$l”"
                BlockKind.DIVIDER -> "──────────"
                else -> indented
            }
        }.joinToString("\n").also { }
    }

    /** Entry point for Home preview: never emits raw markers. */
    fun previewText(raw: String, format: String): String {
        if (format == FORMAT) {
            val c = decodeOrNull(raw) ?: return stripSyntax(raw).take(400)
            if (c.text.isBlank()) return ""
            return plain(c).take(400)
        }
        return stripSyntax(raw).take(400)
    }

    /** Remove legacy markers, keeping readable text (used for preview + migration base). */
    fun stripSyntax(raw: String): String {
        return raw.lines().joinToString("\n") { line ->
            var l = line.trimStart()
            l = CHECK_RE.replace(l) { if (it.groupValues[1].lowercase() == "x") "☑ " else "☐ " }
            l = BULLET_RE.replace(l, "• ")
            l = NUMBERED_RE.replace(l, "")
            var s = l
            s = s.replace("<u>", "").replace("</u>", "").replace("<U>", "").replace("</U>", "")
            s = BOLD_RE.replace(s, "$1")
            s = STRIKE_RE.replace(s, "$1")
            s = UNDER_RE.replace(s, "$1")
            s = ITAL_RE.replace(s, "$1")
            s
        }
    }

    private val CHECK_RE = Regex("""^-\s\[([ xX])\]\s?""")
    private val BULLET_RE = Regex("""^[-*]\s+""")
    private val NUMBERED_RE = Regex("""^\d+[.)]\s+""")
    private val BOLD_RE = Regex("""\*\*(.+?)\*\*""")
    private val STRIKE_RE = Regex("""~~(.+?)~~""")
    private val UNDER_RE = Regex("""__(.+?)__""")
    private val ITAL_RE = Regex("""(?<!\w)_([^_]+?)_(?!\w)""")

    /**
     * Migrate legacy content to [RichContent]. Markers become spans/blocks;
     * anything ambiguous is preserved as plain paragraph text — never dropped.
     */
    fun migrate(raw: String, format: String): RichContent {
        if (format == FORMAT) {
            val c = decodeOrNull(raw)
            if (c != null) return c
            // Corrupt rich payload: preserve text, drop structure (text is never lost).
            return RichContent(stripSyntax(raw))
        }
        val out = StringBuilder()
        val spans = mutableListOf<RichSpan>()
        val blocks = mutableListOf<RichBlock>()
        raw.lines().forEachIndexed { i, line ->
            var l = line
            var kind = BlockKind.PARAGRAPH
            var checked = false
            var m = CHECK_RE.find(l)
            if (m != null) {
                kind = BlockKind.CHECK
                checked = m.groupValues[1].lowercase() == "x"
                l = l.substring(m.value.length)
            } else if (BULLET_RE.containsMatchIn(l.trimStart())) {
                kind = BlockKind.BULLET
                l = BULLET_RE.replaceFirst(l.trimStart(), "")
            } else {
                val nm = NUMBERED_RE.find(l.trimStart())
                if (nm != null) {
                    kind = BlockKind.NUMBERED
                    l = l.trimStart().substring(nm.value.length)
                }
            }
            val base = out.length
            val parsed = parseInline(l, base, spans)
            if (i > 0) out.append('\n')
            out.append(parsed)
            if (kind != BlockKind.PARAGRAPH) blocks.add(RichBlock(i, kind, checked))
        }
        return RichContent(out.toString(), spans, blocks)
    }

    /** Parse inline markers in one (prefix-stripped) line; appends clean text, records absolute spans. */
    private fun parseInline(line: String, base: Int, spans: MutableList<RichSpan>): String {
        var s = line
            .replace("<u>", "\u0001").replace("</u>", "\u0002")
            .replace("<U>", "\u0001").replace("</U>", "\u0002")
        val out = StringBuilder()
        val stack = mutableListOf<Pair<String, Int>>() // marker -> relative start
        var i = 0
        fun rel() = out.length
        while (i < s.length) {
            when {
                s.startsWith("**", i) -> {
                    val top = stack.lastOrNull()
                    if (top != null && top.first == "**") {
                        stack.removeLast()
                        spans.add(RichSpan(base + top.second, base + rel(), SpanKind.BOLD))
                    } else stack.add("**" to rel())
                    i += 2
                }
                s.startsWith("~~", i) -> {
                    val top = stack.lastOrNull()
                    if (top != null && top.first == "~~") {
                        stack.removeLast()
                        spans.add(RichSpan(base + top.second, base + rel(), SpanKind.STRIKE))
                    } else stack.add("~~" to rel())
                    i += 2
                }
                s.startsWith("__", i) -> {
                    val top = stack.lastOrNull()
                    if (top != null && top.first == "__") {
                        stack.removeLast()
                        spans.add(RichSpan(base + top.second, base + rel(), SpanKind.UNDERLINE))
                    } else stack.add("__" to rel())
                    i += 2
                }
                s[i] == '￾' -> { i += 1 } // safety: ignore stray control chars
                s[i] == '\u0001' -> { stack.add("u" to rel()); i += 1 }
                s[i] == '\u0002' -> {
                    val idx = stack.indexOfLast { it.first == "u" }
                    if (idx >= 0) {
                        val st = stack.removeAt(idx)
                        spans.add(RichSpan(base + st.second, base + rel(), SpanKind.UNDERLINE))
                    }
                    i += 1
                }
                s[i] == '_' -> {
                    // Single-underscore italic: pair within this line.
                    val close = s.indexOf('_', i + 1)
                    if (close > i + 1 && stack.none { it.first == "_" }) {
                        stack.add("_" to rel())
                    } else {
                        val idx = stack.indexOfLast { it.first == "_" }
                        if (idx >= 0) {
                            val st = stack.removeAt(idx)
                            spans.add(RichSpan(base + st.second, base + rel(), SpanKind.ITALIC))
                        } else out.append('_')
                    }
                    i += 1
                }
                else -> { out.append(s[i]); i += 1 }
            }
        }
        // Unclosed markers are dropped (their text is kept) — never emit syntax.
        return out.toString()
    }
}
