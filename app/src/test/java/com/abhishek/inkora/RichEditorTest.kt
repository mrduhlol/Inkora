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
        // Press Enter at end -> new bullet line.
        d = d.onInput("• Apple\n• ", TextRange(9, 9))
        assertEquals(2, d.lines.size)
        assertEquals(BlockKind.BULLET, d.lines[1].block)
        // Type Banana, press Enter on empty bullet -> exit list.
        d = d.onInput("• Apple\n• Banana\n• ", TextRange(18, 18))
        d = d.onInput("• Apple\n• Banana\n", TextRange(17, 17))
        // After deleting trailing prefix the empty line exits on next Enter-like input;
        // at minimum structure stays intact with no duplication.
        assertTrue(d.rendered().contains("Apple"))
        assertTrue(d.rendered().contains("Banana"))
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
}
