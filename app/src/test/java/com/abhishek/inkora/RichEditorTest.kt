package com.abhishek.inkora

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import com.abhishek.inkora.domain.model.BlockKind
import com.abhishek.inkora.domain.model.RichContent
import com.abhishek.inkora.domain.model.RichText
import com.abhishek.inkora.domain.model.SpanKind
import com.abhishek.inkora.features.editor.RichDoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Selection-behavior tests from the spec (Tests 1–5) + list/enter/checkbox flows. */
class RichEditorTest {

    private fun docOf(text: String, sel: TextRange): RichDoc =
        RichDoc.fromRich(RichContent(text), sel)

    @Test fun test1_boldOnlySelectedWord() {
        // "Hello World", select "World" (6..11), Bold.
        val d = docOf("Hello World", TextRange(6, 11)).toggleSpan(SpanKind.BOLD)
        val rich = d.toRich()
        assertEquals("Hello World", rich.text)
        assertEquals(1, rich.spans.size)
        assertEquals(6, rich.spans[0].start)
        assertEquals(11, rich.spans[0].end)
    }

    @Test fun test2_underlineOnlySelectedWord() {
        val d = docOf("Hello", TextRange(0, 5)).toggleSpan(SpanKind.UNDERLINE)
        assertEquals(SpanKind.UNDERLINE, d.toRich().spans.single().kind)
        assertEquals("Hello", d.toRich().text)
    }

    @Test fun test3_bulletAcrossThreeParagraphs() {
        val d = docOf("First\nSecond\nThird", TextRange(0, 18)).toggleBlock(BlockKind.BULLET)
        assertEquals("• First\n• Second\n• Third", d.rendered())
        // Each item on its own line, no malformed combos.
        assertTrue(d.toRich().text.lines().size == 3)
    }

    @Test fun test4_collapsedBold_setsPending_notExistingText() {
        val d0 = docOf("Hello", TextRange(2, 2)).toggleSpan(SpanKind.BOLD)
        assertTrue(d0.toRich().spans.isEmpty()) // existing text untouched
        // Typing "X" at cursor applies pending bold to the new char only.
        val rendered = d0.rendered()
        val typed = rendered.substring(0, 2) + "X" + rendered.substring(2)
        val d1 = d0.onInput(typed, TextRange(3, 3))
        val spans = d1.toRich().spans
        assertEquals(1, spans.size)
        assertEquals(2, spans[0].start)
        assertEquals(3, spans[0].end)
    }

    @Test fun test5_formatDoesNotLeakAcrossParagraphs() {
        val d = docOf("One\nTwo", TextRange(0, 3)).toggleSpan(SpanKind.BOLD)
        val spans = d.toRich().spans
        assertEquals(1, spans.size)
        assertTrue(spans[0].end <= 3)
    }

    @Test fun bulletEnter_continuesThenExits() {
        var d = docOf("Apple", TextRange(5, 5)).toggleBlock(BlockKind.BULLET)
        assertEquals("• Apple", d.rendered())
        // Enter at end of "• Apple" (cursor 7): field becomes "• Apple\n", cursor 8.
        d = d.onInput("• Apple\n", TextRange(8, 8))
        assertEquals(2, d.lines.size)
        assertEquals(BlockKind.BULLET, d.lines[1].block)
        assertEquals("• Apple\n• ", d.rendered())
        // Caret sits after the generated prefix: typing lands in content.
        assertEquals(10, d.selection.start)
        // Type Banana (cursor 10 -> 16), then Enter -> third bullet.
        d = d.onInput("• Apple\n• Banana", TextRange(16, 16))
        assertEquals("Banana", d.lines[1].text)
        d = d.onInput("• Apple\n• Banana\n", TextRange(17, 17))
        assertEquals(BlockKind.BULLET, d.lines[2].block)
        // Enter on the empty bullet exits the list.
        d = d.onInput("• Apple\n• Banana\n• \n", TextRange(20, 20))
        assertEquals(BlockKind.PARAGRAPH, d.lines[2].block)
        assertEquals("Apple\nBanana\n\n", d.toRich().text)
    }

    @Test fun numbered_rendersSequentially() {
        val d = docOf("Apple\nBanana\nOrange", TextRange(0, 20)).toggleBlock(BlockKind.NUMBERED)
        assertEquals("1. Apple\n2. Banana\n3. Orange", d.rendered())
    }

    @Test fun checklist_toggleAndRender() {
        var d = docOf("Study physics", TextRange(0, 14)).toggleBlock(BlockKind.CHECK)
        assertEquals("☐ Study physics", d.rendered())
        d = d.toggleCheck(0)
        assertEquals("☑ Study physics", d.rendered())
        // Stored text has no glyphs or brackets.
        assertEquals("Study physics", d.toRich().text)
    }

    @Test fun toolbarActiveKinds_reflectSelection() {
        val d = docOf("Hello World", TextRange(6, 11)).toggleSpan(SpanKind.BOLD)
        assertTrue(d.copy(selection = TextRange(6, 11)).activeKinds().contains(SpanKind.BOLD))
    }

    @Test fun render_usesNoSyntaxMarkers() {
        val rich = RichText.migrate("**Bold** _it_\n- [ ] task\n1. one", "md-v1")
        val d = RichDoc.fromRich(rich)
        val r = d.render(Color.Black).text
        assertTrue(!r.contains("**") && !r.contains("[ ]") && !r.contains("_it_"))
    }

    @Test fun heading_cycle_body_h1_h2_h3_body() {
        var d = docOf("Title", TextRange(0, 5))
        d = d.cycleHeading()
        assertEquals(BlockKind.HEADING1, d.lines[0].block)
        d = d.cycleHeading()
        assertEquals(BlockKind.HEADING2, d.lines[0].block)
        d = d.cycleHeading()
        assertEquals(BlockKind.HEADING3, d.lines[0].block)
        d = d.cycleHeading()
        assertEquals(BlockKind.PARAGRAPH, d.lines[0].block)
        assertEquals("Title", d.toRich().text) // no # markers
    }

    @Test fun quote_toggle_and_render() {
        var d = docOf("Important", TextRange(0, 9)).toggleQuote()
        assertEquals(BlockKind.QUOTE, d.lines[0].block)
        assertTrue(d.render(Color.Black).text.contains("Important"))
        d = d.toggleQuote()
        assertEquals(BlockKind.PARAGRAPH, d.lines[0].block)
    }

    @Test fun divider_insert_and_atomic_edit() {
        var d = docOf("Above", TextRange(5, 5)).insertDivider()
        assertEquals(2, d.lines.size)
        assertEquals(BlockKind.DIVIDER, d.lines[1].block)
        assertTrue(!d.rendered().contains("---"))
        // Typing over a divider converts it to a paragraph.
        d = d.onInput(d.rendered() + "x", TextRange(d.rendered().length + 1, d.rendered().length + 1))
        assertEquals(BlockKind.PARAGRAPH, d.lines[1].block)
    }

    @Test fun align_cycle_and_indent_bounds() {
        var d = docOf("Text", TextRange(0, 4)).cycleAlign()
        assertEquals(com.abhishek.inkora.domain.model.ParaAlign.CENTER, d.lines[0].align)
        d = d.indentMore().indentMore()
        assertEquals(2, d.lines[0].indent)
        d = d.indentLess().indentLess().indentLess()
        assertEquals(0, d.lines[0].indent)
    }

    @Test fun enter_onHeading_breaksToParagraph() {
        var d = docOf("Head\nBody", TextRange(4, 4)).toggleBlock(BlockKind.HEADING1)
        // Enter at end of heading line: new line is a paragraph.
        d = d.onInput("Head\n\nBody", TextRange(5, 5))
        assertEquals(BlockKind.PARAGRAPH, d.lines[1].block)
        assertEquals(BlockKind.PARAGRAPH, d.lines[2].block)
    }

    @Test fun activeBlocks_uniformSelection() {
        val d = docOf("A\nB", TextRange(0, 3)).toggleBlock(BlockKind.BULLET)
        assertTrue(d.copy(selection = TextRange(0, 3)).activeBlocks().contains(BlockKind.BULLET))
    }

    @Test fun activeBlocks_mixedOrParagraph_yieldsEmpty() {
        val d = docOf("A\nB", TextRange(0, 3))
        assertTrue(d.activeBlocks().isEmpty())
        val one = docOf("A\nB", TextRange(0, 1)).toggleBlock(BlockKind.BULLET)
        assertTrue(one.copy(selection = TextRange(0, 5)).activeBlocks().isEmpty())
    }

    @Test fun link_attachRemoveAndLookup() {
        var d = docOf("OpenAI rules", TextRange(0, 6)).setLink("https://openai.com")
        assertEquals("OpenAI rules", d.toRich().text)
        assertEquals("https://openai.com", d.linkAtSelection()?.url)
        // Links survive typing elsewhere.
        val typed = d.rendered().replace("rules", "rules!")
        d = d.onInput(typed, TextRange(typed.length, typed.length))
        assertEquals(1, d.toRich().links.size)
        // Remove via selection.
        d = d.copy(selection = TextRange(0, 6)).removeLink()
        assertTrue(d.toRich().links.isEmpty())
    }

    @Test fun link_collapsedIsNoop() {
        val d = docOf("Hello", TextRange(2, 2)).setLink("https://x.com")
        assertTrue(d.toRich().links.isEmpty())
        assertEquals(null, docOf("Hello", TextRange(2, 2)).linkAtSelection())
    }

    @Test fun code_toggleEnterAndExit() {
        var d = docOf("print(1)", TextRange(0, 8)).toggleBlock(BlockKind.CODE)
        assertEquals(BlockKind.CODE, d.lines[0].block)
        assertTrue(d.activeBlocks().contains(BlockKind.CODE))
        // Enter continues the code block.
        d = d.onInput("print(1)\n", TextRange(9, 9))
        assertEquals(BlockKind.CODE, d.lines[1].block)
        // Enter on the blank code line exits to paragraph.
        d = d.onInput("print(1)\n\n", TextRange(10, 10))
        assertEquals(BlockKind.PARAGRAPH, d.lines[1].block)
    }

    @Test fun table_insertEditEnterAndExit() {
        var d = docOf("", TextRange(0, 0)).insertTable(2, 2)
        assertEquals(2, d.lines.size)
        assertTrue(d.lines.all { it.block == BlockKind.TABLE })
        val group = d.tableGroupAtCursor()
        assertTrue(group != null)
        // Structured cell edit.
        d = d.setTableCells(group!!, listOf(listOf("Physics", "85"), listOf("Maths", "90")))
        assertEquals("Physics │ 85", d.lines[0].text)
        assertEquals("Maths │ 90", d.lines[1].text)
        assertEquals(listOf(listOf("Physics", "85"), listOf("Maths", "90")), d.tableCells(group))
        // Enter at end of last row adds an empty row of equal width.
        val rendered = d.rendered()
        d = d.onInput("$rendered\n", TextRange(rendered.length + 1, rendered.length + 1))
        assertEquals(3, d.lines.size)
        assertEquals(" │ ", d.lines[2].text)
        // Enter on the blank row exits the table.
        val rendered2 = d.rendered()
        d = d.onInput("$rendered2\n", TextRange(rendered2.length + 1, rendered2.length + 1))
        assertEquals(BlockKind.PARAGRAPH, d.lines[2].block)
    }

    @Test fun contentBounds_excludesGeneratedPrefix() {
        val d = docOf("Apple\nBanana", TextRange(0, 12)).toggleBlock(BlockKind.BULLET)
        // Rendered: "• Apple\n• Banana" — content of line 0 starts after "• ".
        assertEquals(2 to 7, d.contentBounds(0))
        assertEquals(10 to 16, d.contentBounds(1))
        assertEquals(null, d.contentBounds(5))
    }

    @Test fun size_appliesOnlyToSelection() {
        var d = docOf("Hello World", TextRange(6, 11)).applySize(20)
        assertEquals("Hello World", d.toRich().text)
        assertEquals(20, d.toRich().spans.single().sizeSp)
        assertEquals(20, d.copy(selection = TextRange(6, 11)).activeSize())
        // Clearing restores default.
        d = d.copy(selection = TextRange(6, 11)).applySize(null)
        assertTrue(d.toRich().spans.none { it.kind == SpanKind.SIZE })
    }

    @Test fun size_mixedSelectionReportsNull() {
        val d = docOf("Hello World", TextRange(0, 5)).applySize(12)
        assertEquals(null, d.copy(selection = TextRange(0, 11)).activeSize())
        assertEquals(12, d.copy(selection = TextRange(0, 5)).activeSize())
    }

    @Test fun size_collapsedStagesPendingForTypedText() {
        val d0 = docOf("Hi", TextRange(1, 1)).applySize(24)
        assertTrue(d0.toRich().spans.isEmpty())
        val typed = "HXi"
        val d1 = d0.onInput(typed, TextRange(2, 2))
        val spans = d1.toRich().spans.filter { it.kind == SpanKind.SIZE }
        assertEquals(1, spans.size)
        assertEquals(1 to 2, spans.single().start to spans.single().end)
    }

    @Test fun color_appliesClearsAndReports() {
        var d = docOf("Hello", TextRange(0, 5)).applyColor(-65536)
        assertEquals(-65536, d.toRich().spans.single().colorArgb)
        assertEquals(-65536, d.copy(selection = TextRange(0, 5)).activeColor())
        d = d.copy(selection = TextRange(0, 2)).applyColor(null)
        assertEquals(1, d.toRich().spans.size) // survivor on 2..5
        d = d.copy(selection = TextRange(0, 5)).applyColor(null)
        assertTrue(d.toRich().spans.isEmpty())
    }
}
