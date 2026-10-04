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
}
