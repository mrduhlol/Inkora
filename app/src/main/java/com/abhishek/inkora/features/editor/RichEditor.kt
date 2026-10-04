package com.abhishek.inkora.features.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.abhishek.inkora.domain.model.BlockKind
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
    val checked: Boolean = false
)

data class RichDoc(
    val lines: List<EditLine> = listOf(EditLine()),
    val selection: TextRange = TextRange.Zero,
    val pending: Set<SpanKind> = emptySet()
) {
    companion object {
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
                    EditLine(t, rel, b?.kind ?: BlockKind.PARAGRAPH, b?.checked ?: false)
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
        lines.forEachIndexed { i, l ->
            if (i > 0) sb.append('\n')
            val base = sb.length
            sb.append(l.text)
            l.spans.forEach { spans.add(RichSpan(base + it.start, base + it.end, it.kind)) }
            if (l.block != BlockKind.PARAGRAPH) blocks.add(RichBlock(i, l.block, l.checked))
        }
        return RichContent(sb.toString(), spans, blocks)
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

    fun prefixFor(l: EditLine, number: Int): String = when (l.block) {
        BlockKind.BULLET -> "• "
        BlockKind.NUMBERED -> "$number. "
        BlockKind.CHECK -> (if (l.checked) "☑ " else "☐ ")
        BlockKind.PARAGRAPH -> ""
    }

    fun render(prefixColor: Color): AnnotatedString {
        var number = 0
        return buildAnnotatedString {
            lines.forEachIndexed { i, l ->
                if (i > 0) append("\n")
                if (l.block == BlockKind.NUMBERED) number += 1
                val p = prefixFor(l, number)
                if (p.isNotEmpty()) {
                    pushStyle(SpanStyle(color = prefixColor))
                    append(p)
                    pop()
                }
                val base = length
                append(l.text)
                l.spans.forEach { s ->
                    addStyle(styleFor(s.kind), base + s.start, base + s.end)
                }
            }
        }
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

    /** Enter continuation: null = not a simple Enter (use generic path). */
    private fun handleEnter(newRendered: String, cursor: TextRange, newSel: TextRange): RichDoc? {
        val newLines = newRendered.split('\n')
        if (newLines.size != lines.size + 1) return null
        val newIdx = lineIndexAtRenderedText(newRendered, cursor.start)
        val srcIdx = newIdx - 1
        if (srcIdx < 0 || srcIdx >= lines.size) return null
        val src = lines[srcIdx]
        if (src.block == BlockKind.PARAGRAPH) return null
        val srcText = newLines[srcIdx]
        val strippedSrc = stripExpected(srcText, src)
        return if (strippedSrc.isBlank()) {
            // Empty item -> exit list: source line becomes paragraph, caret to its start.
            // Re-parse remaining lines generically, then fix the affected line.
            val base = onInputGeneric(newRendered, newSel)
            val fixed = base.lines.toMutableList()
            if (srcIdx < fixed.size) fixed[srcIdx] = fixed[srcIdx].copy(block = BlockKind.PARAGRAPH, checked = false)
            val withFixed = base.copy(lines = fixed)
            withFixed.copy(selection = TextRange(withFixed.renderedLineStart(withFixed.rendered(), srcIdx)))
        } else {
            val base = onInputGeneric(newRendered, newSel)
            val fixed = base.lines.toMutableList()
            if (newIdx < fixed.size) {
                fixed[newIdx] = fixed[newIdx].copy(block = src.block, checked = false, spans = emptyList())
            }
            // Caret goes after the generated prefix so typing lands in content.
            val withFixed = base.copy(lines = fixed)
            val r = withFixed.rendered()
            val ls = withFixed.renderedLineStart(r, newIdx)
            withFixed.copy(selection = TextRange(ls + withFixed.prefixLenAt(newIdx)))
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
        // Numbered prefix varies; match generically.
        val noNum = Regex("""^\d+\.\s+""").replaceFirst(raw, "")
        if (line.block == BlockKind.NUMBERED) return noNum
        val p = when (line.block) {
            BlockKind.BULLET -> "• "
            BlockKind.CHECK -> if (raw.startsWith("☑ ")) "☑ " else "☐ "
            else -> ""
        }
        return if (p.isNotEmpty() && raw.startsWith(p)) raw.substring(p.length) else raw
    }

    /**
     * Parse one edited rendered line back into content + block.
     * Generated prefixes are stripped wherever they moved; the block is kept by
     * line slot, so user edits never silently change list structure.
     */
    private fun parseLine(raw: String, old: EditLine?): EditLine {
        if (old == null || old.block == BlockKind.PARAGRAPH) return EditLine(raw, emptyList())
        var text = raw
        val candidates = mutableListOf<String>()
        when (old.block) {
            BlockKind.BULLET -> candidates.add("• ")
            BlockKind.NUMBERED -> Regex("""^\d+\.\s+""").find(text)?.value?.let { candidates.add(it) }
            BlockKind.CHECK -> {
                candidates.add(if (old.checked) "☑ " else "☐ ")
                candidates.add(if (old.checked) "☐ " else "☑ ")
            }
            else -> Unit
        }
        val hit = candidates.firstOrNull { it.isNotEmpty() && text.startsWith(it) }
        if (hit != null) {
            text = text.substring(hit.length)
        } else {
            // Prefix moved mid-line (typed before it) or partially deleted: remove it,
            // else keep text as-is. Block is preserved either way.
            candidates.firstOrNull { it.isNotEmpty() && text.contains(it) }?.let {
                text = text.replaceFirst(it, "")
            }
        }
        return EditLine(text, remapSpans(old, text), old.block, old.checked)
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
}

fun styleFor(kind: SpanKind): SpanStyle = when (kind) {
    SpanKind.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
    SpanKind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    SpanKind.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
    SpanKind.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
}
