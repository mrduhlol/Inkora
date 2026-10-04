package com.abhishek.inkora.features.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.abhishek.inkora.domain.model.BlockKind
import com.abhishek.inkora.domain.model.ParaAlign
import com.abhishek.inkora.domain.model.RichAlign
import com.abhishek.inkora.domain.model.RichBlock
import com.abhishek.inkora.domain.model.RichContent
import com.abhishek.inkora.domain.model.RichSpan
import com.abhishek.inkora.domain.model.RichText
import com.abhishek.inkora.domain.model.SpanKind

/**
 * Editable rich document: line-structured model rendered as ONE AnnotatedString
 * so selection can span multiple lines/paragraphs.
 *
 * List prefixes (•, 1., ☐/☑) are generated glyphs managed by code — never
 * user-typed syntax. [pending] holds span kinds to apply to subsequently typed
 * text when the cursor is collapsed (Test 4).
 */
data class EditLine(
    val text: String = "",
    val spans: List<RichSpan> = emptyList(), // relative to [text]
    val block: BlockKind = BlockKind.PARAGRAPH,
    val checked: Boolean = false,
    val indent: Int = 0, // 0..4, rendered as em-space prefix
    val align: ParaAlign = ParaAlign.LEFT
)

fun ParaAlign.toTextAlign(): TextAlign = when (this) {
    ParaAlign.LEFT -> TextAlign.Left
    ParaAlign.CENTER -> TextAlign.Center
    ParaAlign.RIGHT -> TextAlign.Right
    ParaAlign.JUSTIFY -> TextAlign.Justify
}

data class RichDoc(
    val lines: List<EditLine> = listOf(EditLine()),
    val selection: TextRange = TextRange.Zero,
    val pending: Set<SpanKind> = emptySet()
) {
    companion object {
        /** Invisible indent prefix: em-spaces, never user syntax. */
        fun indentStr(indent: Int): String = "\u2003\u2003".repeat(indent.coerceIn(0, 4))

        fun fromRich(c: RichContent, selection: TextRange = TextRange.Zero): RichDoc {
            val raw = RichText.linesOf(c.text)
            return RichDoc(
                lines = raw.mapIndexed { i, t ->
                    val lineStart = c.text.lineStartOf(i)
                    val rel = c.spans.mapNotNull {
                        val s = (it.start - lineStart).coerceIn(0, t.length)
                        val e = (it.end - lineStart).coerceIn(0, t.length)
                        if (s < e) RichSpan(s, e, it.kind) else null
                    }
                    val b = c.blocks.firstOrNull { it.line == i }
                    val a = c.aligns.firstOrNull { it.line == i }
                    EditLine(
                        t, rel, b?.kind ?: BlockKind.PARAGRAPH, b?.checked ?: false,
                        b?.indent?.coerceIn(0, 4) ?: 0, a?.align ?: ParaAlign.LEFT
                    )
                }.ifEmpty { listOf(EditLine()) },
                selection = selection
            )
        }

        private fun String.lineStartOf(line: Int): Int {
            var pos = 0
            var cur = 0
            while (cur < line) {
                val nl = indexOf('\n', pos)
                if (nl < 0) return length
                pos = nl + 1
                cur++
            }
            return pos
        }
    }

    fun toRich(): RichContent {
        val sb = StringBuilder()
        val spans = mutableListOf<RichSpan>()
        val blocks = mutableListOf<RichBlock>()
        val aligns = mutableListOf<RichAlign>()
        lines.forEachIndexed { i, l ->
            if (i > 0) sb.append('\n')
            val base = sb.length
            sb.append(l.text)
            l.spans.forEach { spans.add(RichSpan(base + it.start, base + it.end, it.kind)) }
            if (l.block != BlockKind.PARAGRAPH || l.indent != 0) {
                blocks.add(RichBlock(i, l.block, l.checked, l.indent.coerceIn(0, 4)))
            }
            if (l.align != ParaAlign.LEFT) aligns.add(RichAlign(i, l.align))
        }
        return RichContent(sb.toString(), spans, blocks, aligns)
    }

    /** Rendered text (with generated prefixes) — the string BasicTextField edits. */
    fun rendered(): String = buildString {
        var n = 0
        lines.forEachIndexed { i, l ->
            if (i > 0) append('\n')
            if (l.block == BlockKind.NUMBERED) n += 1
            append(prefixFor(l, n))
            append(l.text)
        }
    }

    fun prefixFor(l: EditLine, number: Int): String {
        // Em-space indent: invisible formatting, distinct from user-typed spaces.
        val indent = indentStr(l.indent)
        val kind = when (l.block) {
            BlockKind.BULLET -> "• "
            BlockKind.NUMBERED -> "$number. "
            BlockKind.CHECK -> (if (l.checked) "☑ " else "☐ ")
            BlockKind.QUOTE -> "┃ "
            BlockKind.DIVIDER -> ""
            else -> ""
        }
        return indent + kind
    }

    fun render(prefixColor: Color, accent: Color = prefixColor): AnnotatedString {
        var number = 0
        return buildAnnotatedString {
            lines.forEachIndexed { i, l ->
                if (i > 0) append("\n")
                if (l.block == BlockKind.NUMBERED) number += 1
                val lineStart = length
                if (l.block == BlockKind.DIVIDER) {
                    pushStyle(SpanStyle(color = prefixColor))
                    append("──────────")
                    pop()
                } else {
                    val p = prefixFor(l, number)
                    if (p.isNotEmpty()) {
                        // Quote bar uses the accent; other glyphs stay muted.
                        val endsWithBar = p.endsWith("┃ ")
                        if (endsWithBar) {
                            val ind = p.substring(0, p.length - 2)
                            if (ind.isNotEmpty()) {
                                pushStyle(SpanStyle(color = prefixColor)); append(ind); pop()
                            }
                            pushStyle(SpanStyle(color = accent)); append("┃ "); pop()
                        } else {
                            pushStyle(SpanStyle(color = prefixColor)); append(p); pop()
                        }
                    }
                    val base = length
                    append(l.text)
                    lineStyleFor(l)?.let { addStyle(it, base, base + l.text.length) }
                    l.spans.forEach { s ->
                        addStyle(styleFor(s.kind), base + s.start, base + s.end)
                    }
                }
                if (l.align != ParaAlign.LEFT) {
                    addStyle(ParagraphStyle(textAlign = l.align.toTextAlign()), lineStart, length)
                }
            }
        }
    }

    private fun lineStyleFor(l: EditLine): SpanStyle? = when (l.block) {
        BlockKind.HEADING1 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp)
        BlockKind.HEADING2 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp)
        BlockKind.HEADING3 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp)
        BlockKind.QUOTE -> SpanStyle(fontStyle = FontStyle.Italic)
        else -> null
    }

    /** Absolute prefix ranges in rendered text (for checkbox tap detection). */
    fun prefixRanges(): List<IntRange> {
        var number = 0
        var pos = 0
        return lines.map { l ->
            if (l.block == BlockKind.NUMBERED) number += 1
            val p = prefixFor(l, number)
            val r = pos until pos + p.length
            pos += p.length + l.text.length + 1
            r
        }
    }

    fun lineIndexAtRendered(offset: Int): Int {
        val t = rendered()
        val o = offset.coerceIn(0, t.length)
        return t.substring(0, o).count { it == '\n' }
    }

    // ---------- input: parse edited text back into lines ----------

    fun onInput(newRendered: String, newSel: TextRange): RichDoc {
        val oldRendered = rendered()
        // Single Enter press? Handle list continuation BEFORE generic remap.
        if (newRendered.length == oldRendered.length + 1) {
            val diff = firstDiff(oldRendered, newRendered)
            if (diff >= 0 && newRendered.getOrNull(diff) == '\n') {
                val continued = handleEnter(newRendered, TextRange(diff + 1), newSel)
                if (continued != null) return continued
            }
        }
        val rawLines = newRendered.split('\n')
        val parsed = rawLines.mapIndexed { i, raw -> parseLine(raw, lines.getOrNull(i)) }
        // Apply pending attrs to newly inserted chars.
        var doc = copy(lines = parsed.ifEmpty { listOf(EditLine()) }, selection = newSel, pending = pending)
        if (pending.isNotEmpty()) {
            val ins = insertedRange(oldRendered, newRendered)
            if (ins != null && ins.first < ins.second && !newRendered.substring(ins.first, ins.second).contains('\n')) {
                doc = doc.addSpansAbsolute(ins.first, ins.second, pending)
            }
            doc = doc.copy(pending = emptySet())
        }
        return doc
    }

    private fun remapSpans(old: EditLine?, newText: String): List<RichSpan> {
        if (old == null) return emptyList()
        // Spans are stored relative; typing inside a line keeps relative offsets valid
        // because BasicTextField shifts AnnotatedString spans — but we re-parse from raw
        // text, so clamp surviving spans instead. Precise per-keystroke fidelity comes
        // from span preservation in render(); here keep spans that still fit.
        return old.spans.mapNotNull {
            val s = it.start.coerceIn(0, newText.length)
            val e = it.end.coerceIn(0, newText.length)
            if (s < e) RichSpan(s, e, it.kind) else null
        }
    }

    private fun firstDiff(a: String, b: String): Int {
        val n = minOf(a.length, b.length)
        for (i in 0 until n) if (a[i] != b[i]) return i
        return if (a.length == b.length) -1 else n
    }

    private fun insertedRange(old: String, new: String): Pair<Int, Int>? {
        if (new.length <= old.length) return null
        val s = firstDiff(old, new)
        if (s < 0) return null
        val suffix = old.length - s
        val e = new.length - suffix
        return if (e > s) s to e else null
    }

    /**
     * Enter-key split at [newIdx] (new line) from [srcIdx] (source line).
     * Only BULLET/NUMBERED/CHECK continue; headings, quotes, dividers and
     * paragraphs break to plain paragraphs. Old lines map explicitly so later
     * list lines never lose their blocks (no index drift).
     * Null = not a simple Enter (use generic path).
     */
    private fun handleEnter(newRendered: String, cursor: TextRange, newSel: TextRange): RichDoc? {
        val newLines = newRendered.split('\n')
        if (newLines.size != lines.size + 1) return null
        val newIdx = lineIndexAtRenderedText(newRendered, cursor.start)
        val srcIdx = newIdx - 1
        if (srcIdx !in lines.indices || newIdx !in newLines.indices) return null
        val src = lines[srcIdx]
        val oldFor: (Int) -> EditLine? = { i ->
            when {
                i < newIdx -> lines.getOrNull(i)
                i == newIdx -> null
                else -> lines.getOrNull(i - 1)
            }
        }
        val parsed = newLines.mapIndexed { i, raw -> parseLine(raw, oldFor(i)) }.toMutableList()
        val srcText = stripExpected(newLines[srcIdx], src)
        return when (src.block) {
            BlockKind.BULLET, BlockKind.NUMBERED, BlockKind.CHECK -> {
                if (srcText.isBlank()) {
                    // Empty item -> exit list.
                    parsed[srcIdx] = parsed[srcIdx].copy(block = BlockKind.PARAGRAPH, checked = false)
                    val withFixed = copy(lines = parsed.ifEmpty { listOf(EditLine()) })
                    withFixed.copy(
                        selection = TextRange(withFixed.renderedLineStart(withFixed.rendered(), srcIdx))
                    )
                } else {
                    parsed[newIdx] = parsed[newIdx].copy(block = src.block, checked = false, spans = emptyList())
                    val withFixed = copy(lines = parsed)
                    val r = withFixed.rendered()
                    val ls = withFixed.renderedLineStart(r, newIdx)
                    withFixed.copy(selection = TextRange(ls + withFixed.prefixLenAt(newIdx)))
                }
            }
            else -> {
                // Headings, quotes, dividers, paragraphs: new line stays a paragraph.
                val withFixed = copy(lines = parsed.ifEmpty { listOf(EditLine()) })
                withFixed.copy(selection = newSel)
            }
        }
    }

    /** Length of the generated prefix on [line] in the current render. */
    fun prefixLenAt(line: Int): Int {
        var n = 0
        lines.forEachIndexed { i, l ->
            if (l.block == BlockKind.NUMBERED) n += 1
            if (i == line) return prefixFor(l, n).length
        }
        return 0
    }

    fun renderedLineStart(r: String, line: Int): Int {
        var pos = 0
        var cur = 0
        while (cur < line) {
            val nl = r.indexOf('\n', pos)
            if (nl < 0) return r.length
            pos = nl + 1
            cur++
        }
        return pos
    }

    private fun lineIndexAtRenderedText(t: String, offset: Int): Int {
        val o = offset.coerceIn(0, t.length)
        return t.substring(0, o).count { it == '\n' }
    }

    private fun stripExpected(raw: String, line: EditLine): String {
        val ind = indentStr(line.indent)
        val body = if (ind.isNotEmpty() && raw.startsWith(ind)) raw.substring(ind.length) else raw
        // Numbered prefix varies; match generically.
        val noNum = Regex("""^\d+\.\s+""").replaceFirst(body, "")
        if (line.block == BlockKind.NUMBERED) return noNum
        val p = when (line.block) {
            BlockKind.BULLET -> "• "
            BlockKind.CHECK -> if (body.startsWith("☑ ")) "☑ " else "☐ "
            BlockKind.QUOTE -> "┃ "
            BlockKind.DIVIDER -> return ""
            else -> ""
        }
        return if (p.isNotEmpty() && body.startsWith(p)) body.substring(p.length) else body
    }

    /**
     * Parse one edited rendered line back into content + structure.
     * Generated prefixes are stripped wherever they moved; block/align/indent
     * are kept by line slot, so user edits never silently change structure.
     * Divider lines are atomic: any typed text converts them to paragraphs.
     */
    private fun parseLine(raw: String, old: EditLine?): EditLine {
        if (old == null) return EditLine(raw, emptyList())
        if (old.block == BlockKind.DIVIDER) {
            return if (raw.isBlank()) old.copy(text = "")
            else EditLine(raw.trim(), emptyList())
        }
        if (old.block == BlockKind.PARAGRAPH && old.indent == 0) {
            return EditLine(raw, remapSpans(old, raw), align = old.align)
        }
        var text = raw
        val ind = indentStr(old.indent)
        if (ind.isNotEmpty() && text.startsWith(ind)) text = text.substring(ind.length)
        val candidates = mutableListOf<String>()
        when (old.block) {
            BlockKind.BULLET -> candidates.add("• ")
            BlockKind.NUMBERED -> Regex("""^\d+\.\s+""").find(text)?.value?.let { candidates.add(it) }
            BlockKind.CHECK -> {
                candidates.add(if (old.checked) "☑ " else "☐ ")
                candidates.add(if (old.checked) "☐ " else "☑ ")
            }
            BlockKind.QUOTE -> candidates.add("┃ ")
            else -> Unit
        }
        val hit = candidates.firstOrNull { it.isNotEmpty() && text.startsWith(it) }
        if (hit != null) {
            text = text.substring(hit.length)
        } else {
            // Prefix moved mid-line (typed before it) or partially deleted: remove it,
            // else keep text as-is. Structure is preserved either way.
            candidates.firstOrNull { it.isNotEmpty() && text.contains(it) }?.let {
                text = text.replaceFirst(it, "")
            }
        }
        return EditLine(text, remapSpans(old, text), old.block, old.checked, old.indent, old.align)
    }

    private fun onInputGeneric(newRendered: String, newSel: TextRange): RichDoc {
        // Same as onInput but without Enter interception (avoids recursion).
        val parsed = newRendered.split('\n').mapIndexed { i, raw -> parseLine(raw, lines.getOrNull(i)) }
        return copy(lines = parsed.ifEmpty { listOf(EditLine()) }, selection = newSel)
    }

    // ---------- formatting ops ----------

    fun toggleSpan(kind: SpanKind): RichDoc {
        val (s, e) = selection.min to selection.max
        if (s >= e) {
            return copy(pending = if (pending.contains(kind)) pending - kind else pending + kind)
        }
        return addOrRemoveAbsolute(s, e, kind).copy(pending = emptySet())
    }

    private fun addSpansAbsolute(s: Int, e: Int, kinds: Set<SpanKind>): RichDoc {
        var doc = this
        kinds.forEach { doc = doc.addOrRemoveAbsolute(s, e, it, addOnly = true) }
        return doc
    }

    private fun addOrRemoveAbsolute(s: Int, e: Int, kind: SpanKind, addOnly: Boolean = false): RichDoc {
        val r = rendered()
        val covered = (s until e).all { i ->
            val li = renderedLineOf(r, i)
            val line = lines[li]
            val rel = i - renderedLineStart(r, li) - linePrefixLen(line, li)
            line.spans.any { it.kind == kind && it.start <= rel && rel < it.end }
        }
        val updated = lines.mapIndexed { li, line ->
            val ls = renderedLineStart(r, li)
            val pl = linePrefixLen(line, li)
            val rs = (s - ls - pl).coerceIn(0, line.text.length)
            val re = (e - ls - pl).coerceIn(0, line.text.length)
            if (rs >= re) return@mapIndexed line
            line.copy(spans = toggleRel(line.spans, rs, re, kind, removeOnly = covered && !addOnly))
        }
        return copy(lines = updated)
    }

    private fun toggleRel(spans: List<RichSpan>, s: Int, e: Int, kind: SpanKind, removeOnly: Boolean): List<RichSpan> {
        val kept = spans.filterNot { it.kind == kind && it.end > s && it.start < e }.toMutableList()
        if (removeOnly) {
            spans.filter { it.kind == kind && it.end > s && it.start < e }.forEach { sp ->
                if (sp.start < s) kept.add(RichSpan(sp.start, s, kind))
                if (sp.end > e) kept.add(RichSpan(e, sp.end, kind))
            }
            return kept.sortedWith(compareBy({ it.start }, { it.end }))
        }
        var ns = s
        var ne = e
        val ov = spans.filter { it.kind == kind && it.end >= s && it.start <= e }
        ov.forEach { ns = minOf(ns, it.start); ne = maxOf(ne, it.end) }
        kept.removeAll(ov.toSet())
        kept.add(RichSpan(ns, ne, kind))
        return kept.sortedWith(compareBy({ it.start }, { it.end }))
    }

    fun toggleBlock(kind: BlockKind): RichDoc {
        val r = rendered()
        val range = RichText.linesInSelection(r, selection.min, selection.max)
        val allSet = range.all { lines.getOrNull(it)?.block == kind }
        val updated = lines.mapIndexed { i, l ->
            if (i in range) l.copy(block = if (allSet) BlockKind.PARAGRAPH else kind, checked = false) else l
        }
        // Keep selection stable across prefix-length changes.
        return copy(lines = updated, selection = shiftSelection(r, updated))
    }

    fun toggleCheck(line: Int): RichDoc {
        val cur = lines.getOrNull(line) ?: return this
        if (cur.block != BlockKind.CHECK) return this
        return copy(lines = lines.mapIndexed { i, l -> if (i == line) l.copy(checked = !l.checked) else l })
    }

    private fun selectedLines(): IntRange {
        val r = rendered()
        return RichText.linesInSelection(r, selection.min, selection.max)
    }

    private fun restyleSelected(map: (EditLine) -> EditLine): RichDoc {
        val r = rendered()
        val range = selectedLines()
        val updated = lines.mapIndexed { i, l -> if (i in range) map(l) else l }
        return copy(lines = updated, selection = shiftSelection(r, updated))
    }

    /** Heading cycle over selected lines: Body → H1 → H2 → H3 → Body. */
    fun cycleHeading(): RichDoc {
        val first = lines.getOrNull(selectedLines().first)?.block
        val next = when (first) {
            BlockKind.HEADING1 -> BlockKind.HEADING2
            BlockKind.HEADING2 -> BlockKind.HEADING3
            BlockKind.HEADING3 -> BlockKind.PARAGRAPH
            else -> BlockKind.HEADING1
        }
        return restyleSelected { it.copy(block = next, checked = false) }
    }

    /** Quote toggle over selected lines. */
    fun toggleQuote(): RichDoc {
        val range = selectedLines()
        val allSet = range.all { lines.getOrNull(it)?.block == BlockKind.QUOTE }
        return restyleSelected {
            it.copy(block = if (allSet) BlockKind.PARAGRAPH else BlockKind.QUOTE, checked = false)
        }
    }

    /**
     * Insert a divider below the current line. Blank current line becomes the
     * divider itself; otherwise a new divider line is inserted after it.
     */
    fun insertDivider(): RichDoc {
        val li = lineIndexAtRendered(selection.min)
        val cur = lines.getOrNull(li) ?: return this
        val updated = lines.toMutableList()
        if (cur.text.isBlank() && cur.block == BlockKind.PARAGRAPH) {
            updated[li] = cur.copy(block = BlockKind.DIVIDER, spans = emptyList())
        } else {
            updated.add(li + 1, EditLine(block = BlockKind.DIVIDER))
        }
        val withLines = copy(lines = updated)
        // Caret to the line after the divider.
        val target = (li + 1).coerceAtMost(withLines.lines.size - 1)
        val nr = withLines.rendered()
        return withLines.copy(selection = TextRange(withLines.renderedLineStart(nr, target)))
    }

    /** Alignment cycle over selected paragraphs: Left → Center → Right → Justify. */
    fun cycleAlign(): RichDoc {
        val first = lines.getOrNull(selectedLines().first)?.align ?: ParaAlign.LEFT
        val next = when (first) {
            ParaAlign.LEFT -> ParaAlign.CENTER
            ParaAlign.CENTER -> ParaAlign.RIGHT
            ParaAlign.RIGHT -> ParaAlign.JUSTIFY
            ParaAlign.JUSTIFY -> ParaAlign.LEFT
        }
        return restyleSelected { it.copy(align = next) }
    }

    fun indentMore(): RichDoc =
        restyleSelected { it.copy(indent = (it.indent + 1).coerceAtMost(4)) }

    fun indentLess(): RichDoc =
        restyleSelected { it.copy(indent = (it.indent - 1).coerceAtLeast(0)) }

    private fun shiftSelection(oldRendered: String, newLines: List<EditLine>): TextRange {
        // Recompute selection by mapping old absolute offsets through line/prefix deltas.
        fun map(offset: Int): Int {
            val li = renderedLineOf(oldRendered, offset)
            val oldLine = lines.getOrNull(li) ?: return offset
            val newLine = newLines.getOrNull(li) ?: return offset
            val oldStart = renderedLineStart(oldRendered, li)
            val rel = (offset - oldStart - linePrefixLen(oldLine, li)).coerceAtLeast(0)
            var pos = 0
            var n = 0
            newLines.forEachIndexed { i, l ->
                if (l.block == BlockKind.NUMBERED) n += 1
                if (i == li) return@map pos + prefixFor(l, n).length + rel.coerceAtMost(l.text.length)
                pos += prefixFor(l, n).length + l.text.length + 1
            }
            return offset
        }
        return TextRange(map(selection.min), map(selection.max))
    }

    private fun renderedLineOf(r: String, offset: Int): Int {
        val o = offset.coerceIn(0, r.length)
        return r.substring(0, o).count { it == '\n' }.coerceIn(0, (lines.size - 1).coerceAtLeast(0))
    }

    private fun linePrefixLen(line: EditLine, index: Int): Int {
        var n = 0
        lines.forEachIndexed { i, l -> if (l.block == BlockKind.NUMBERED) n += 1; if (i == index) return@linePrefixLen prefixFor(l, n).length }
        return prefixFor(line, 1).length
    }

    /** Active span kinds at the collapsed cursor (for toolbar state). */
    fun activeKinds(): Set<SpanKind> {
        val (s, e) = selection.min to selection.max
        if (s < e) {
            val r = rendered()
            return SpanKind.entries.filter { k ->
                (s until e).all { i ->
                    val li = renderedLineOf(r, i)
                    val line = lines[li]
                    val rel = i - renderedLineStart(r, li) - linePrefixLen(line, li)
                    line.spans.any { it.kind == k && it.start <= rel && rel < it.end }
                }
            }.toSet()
        }
        val li = lineIndexAtRendered(selection.start)
        val line = lines.getOrNull(li) ?: return pending
        val r = rendered()
        val rel = selection.start - renderedLineStart(r, li) - linePrefixLen(line, li)
        val atCursor = line.spans.filter { it.start <= rel && rel <= it.end }.map { it.kind }.toSet()
        return atCursor + pending
    }

    /**
     * Block kinds uniformly covering the selection — for toolbar active states.
     * Mixed selections yield an empty set (never claim formatting falsely).
     */
    fun activeBlocks(): Set<BlockKind> {
        val r = rendered()
        val range = RichText.linesInSelection(r, selection.min, selection.max)
        val kinds = range.map { lines.getOrNull(it)?.block ?: BlockKind.PARAGRAPH }.toSet()
        return if (kinds.size == 1 && kinds.single() != BlockKind.PARAGRAPH) kinds else emptySet()
    }
}

fun styleFor(kind: SpanKind): SpanStyle = when (kind) {
    SpanKind.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
    SpanKind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    SpanKind.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
    SpanKind.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
}
