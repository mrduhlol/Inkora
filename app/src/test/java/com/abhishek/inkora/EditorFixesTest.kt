package com.abhishek.inkora

import androidx.compose.ui.text.TextRange
import com.abhishek.inkora.domain.model.BlockKind
import com.abhishek.inkora.domain.model.IMAGE_PLACEHOLDER
import com.abhishek.inkora.domain.model.ImagePlacement
import com.abhishek.inkora.domain.model.RichContent
import com.abhishek.inkora.domain.model.RichText
import com.abhishek.inkora.domain.model.SpanKind
import com.abhishek.inkora.features.editor.RichDoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Editor fixes: double-tap word selection, font size that sticks to
 * everything typed next, and inline images at the caret.
 */
class EditorFixesTest {

    private fun docOf(text: String, sel: TextRange): RichDoc =
        RichDoc.fromRich(RichContent(text), sel)

    // ---------- double tap selects the word ----------

    @Test fun wordRange_coversTheWordOnly() {
        val d = docOf("Hello World", TextRange(0, 11))
        val r = d.wordRangeAt(7, 6, 11)
        assertEquals(TextRange(6, 11), r)
    }

    @Test fun wordRange_neverCoversGeneratedPrefix() {
        val d = docOf("Apple", TextRange(0, 5)).toggleBlock(BlockKind.BULLET)
        val bounds = d.contentBounds(0)!!
        // Whatever the layout reports, the selection stays inside the content.
        val r = d.wordRangeAt(2, 0, 5)
        assertNotNull(r)
        assertTrue(r!!.start >= bounds.first)
        assertTrue(r.end <= bounds.second)
    }

    @Test fun wordRange_trimsSurroundingSpaces() {
        val d = docOf("one two", TextRange(0, 7))
        val r = d.wordRangeAt(4, 3, 8)
        assertEquals(TextRange(4, 7), r)
    }

    @Test fun wordRange_nullWhenNoWordUnderTheOffset() {
        val d = docOf("   ", TextRange(0, 3))
        assertNull(d.wordRangeAt(1, 0, 3))
    }

    @Test fun paragraphRange_isTheLineContent() {
        val d = docOf("Apple\nBanana", TextRange(0, 12)).toggleBlock(BlockKind.BULLET)
        assertEquals(2 to 7, d.contentBounds(0))
    }

    // ---------- font size stays for everything typed next ----------

    @Test fun pendingSize_appliesToEveryTypedCharacter() {
        var d = docOf("Hi", TextRange(2, 2)).applySize(24)
        d = d.onInput("HiA", TextRange(3, 3))
        assertEquals(1, d.toRich().spans.count { it.kind == SpanKind.SIZE })
        // The size is not consumed by the first character: it stays in force.
        d = d.onInput("HiAB", TextRange(4, 4))
        val sizes = d.toRich().spans.filter { it.kind == SpanKind.SIZE }
        assertEquals(2, sizes.size)
        assertTrue(sizes.all { it.sizeSp == 24 })
        assertEquals(24, d.activeSize())
    }

    @Test fun pendingSize_changesWhenTheSizeChanges() {
        var d = docOf("Hi", TextRange(2, 2)).applySize(24)
        d = d.onInput("HiA", TextRange(3, 3))
        d = d.copy(selection = TextRange(3, 3)).applySize(12)
        d = d.onInput("HiAB", TextRange(4, 4))
        val sizes = d.toRich().spans.filter { it.kind == SpanKind.SIZE }
        assertEquals(2, sizes.size)
        assertEquals(12, sizes.last().sizeSp)
        assertEquals(12, d.activeSize())
    }

    @Test fun pendingSize_appliesToSelectionAndKeepsTypingInThatSize() {
        var d = docOf("Hello World", TextRange(6, 11)).applySize(20)
        assertEquals(20, d.toRich().spans.single().sizeSp)
        // Typing right after the formatted word keeps the size.
        d = d.onInput("Hello World!", TextRange(12, 12))
        val sizes = d.toRich().spans.filter { it.kind == SpanKind.SIZE }
        assertEquals(2, sizes.size)
        assertTrue(sizes.all { it.sizeSp == 20 })
    }

    @Test fun pendingColor_alsoSticks() {
        var d = docOf("Hi", TextRange(2, 2)).applyColor(-65536)
        d = d.onInput("HiA", TextRange(3, 3))
        d = d.onInput("HiAB", TextRange(4, 4))
        val colors = d.toRich().spans.filter { it.kind == SpanKind.COLOR }
        assertEquals(2, colors.size)
        assertTrue(colors.all { it.colorArgb == -65536 })
    }

    @Test fun pendingBold_keepsApplyingToNewText() {
        var d = docOf("Hi", TextRange(2, 2)).toggleSpan(SpanKind.BOLD)
        d = d.onInput("HiA", TextRange(3, 3))
        d = d.onInput("HiAB", TextRange(4, 4))
        // Both new characters carry the pending bold (one continuous run).
        val bold = d.toRich().spans.filter { it.kind == SpanKind.BOLD }
        assertEquals(1, bold.size)
        assertEquals(2, bold.single().start)
        assertEquals(4, bold.single().end)
    }

    // ---------- inline images ----------

    @Test fun insertImage_splitsTheLineAtTheCaret() {
        val d = docOf("Hello world", TextRange(5, 5)).insertImage(42L)
        assertEquals(3, d.lines.size)
        assertEquals("Hello", d.lines[0].text)
        assertEquals(IMAGE_PLACEHOLDER, d.lines[1].text)
        assertEquals(" world", d.lines[2].text)
        assertEquals(42L, d.lines[1].image?.attachmentId)
        assertEquals(ImagePlacement.INLINE, d.lines[1].image?.placement)
    }

    @Test fun insertImage_keepsTheImageInTheSavedContent() {
        val d = docOf("Hi", TextRange(2, 2)).insertImage(7L)
        val rich = d.toRich()
        assertTrue(rich.text.contains(IMAGE_PLACEHOLDER))
        assertEquals(1, rich.images.size)
        assertEquals(7L, rich.images.single().attachmentId)
        // Round trip: the picture survives a save/load cycle.
        val back = RichDoc.fromRich(rich)
        assertEquals(1, back.lines.count { it.image != null })
        assertEquals(7L, back.lines.first { it.image != null }.image?.attachmentId)
    }

    @Test fun insertImage_movesTheCaretAfterThePicture() {
        val d = docOf("Hello world", TextRange(5, 5)).insertImage(42L)
        // The caret waits on the text that follows the picture.
        assertEquals(8, d.selection.start)
    }

    @Test fun imagePlacement_floatsOverTheNextParagraph() {
        var d = docOf("First\nSecond", TextRange(0, 0)).insertImage(9L)
        val imageLine = d.lines.indexOfFirst { it.image != null }
        d = d.setImagePlacement(imageLine, ImagePlacement.BEHIND)
        val img = d.lines.first { it.image != null }
        assertEquals(ImagePlacement.BEHIND, img.image?.placement)
        // The picture now shares the line with the text it floats over.
        assertEquals("First", img.text.removePrefix(IMAGE_PLACEHOLDER))
        assertEquals(2, d.lines.size)
    }

    @Test fun imagePlacement_returnsIntoTheFlow() {
        var d = docOf("First\nSecond", TextRange(0, 0)).insertImage(9L)
        val imageLine = d.lines.indexOfFirst { it.image != null }
        d = d.setImagePlacement(imageLine, ImagePlacement.BEHIND)
        val floating = d.lines.indexOfFirst { it.image != null }
        d = d.setImagePlacement(floating, ImagePlacement.INLINE)
        val img = d.lines.first { it.image != null }
        assertEquals(ImagePlacement.INLINE, img.image?.placement)
        assertEquals(IMAGE_PLACEHOLDER, img.text)
    }

    @Test fun imagePlacement_inFrontKeepsTheSameAnchor() {
        var d = docOf("First\nSecond", TextRange(0, 0)).insertImage(9L)
        val imageLine = d.lines.indexOfFirst { it.image != null }
        d = d.setImagePlacement(imageLine, ImagePlacement.IN_FRONT)
        val img = d.lines.first { it.image != null }
        assertEquals(ImagePlacement.IN_FRONT, img.image?.placement)
    }

    @Test fun removeImage_dropsThePlaceholderAndTheReference() {
        var d = docOf("Hi", TextRange(2, 2)).insertImage(3L)
        d = d.removeImage(d.lines.indexOfFirst { it.image != null })
        assertTrue(d.lines.none { it.image != null })
        assertTrue(!d.toRich().text.contains(IMAGE_PLACEHOLDER))
        assertTrue(d.toRich().images.isEmpty())
    }

    @Test fun imageAtCaret_findsThePictureOnTheCaretLine() {        var d = docOf("Hi", TextRange(2, 2)).insertImage(3L)
        val imageLine = d.lines.indexOfFirst { it.image != null }
        d = d.copy(selection = TextRange(d.renderedLineStart(d.rendered(), imageLine)))
        assertEquals(3L, d.imageAtCaret()?.attachmentId)
        assertEquals(imageLine, d.imageLineAtCaret())
    }

    @Test fun imageAnchors_pointAtThePlaceholder() {
        val d = docOf("Hi", TextRange(2, 2)).insertImage(3L)
        val anchors = d.imageAnchors()
        assertEquals(1, anchors.size)
        val r = d.rendered()
        assertEquals(IMAGE_PLACEHOLDER, r.substring(anchors.single().offset, anchors.single().offset + 1))
    }

    @Test fun plainText_marksInlineImages() {
        val d = docOf("Hi", TextRange(2, 2)).insertImage(3L)
        val plain = RichText.plain(d.toRich())
        assertTrue(plain.contains("[image]"))
        assertTrue(!plain.contains(IMAGE_PLACEHOLDER))
    }

    @Test fun image_survivesTypingAroundIt() {
        var d = docOf("Hi", TextRange(2, 2)).insertImage(3L)
        // Typing in the line before the picture must not detach it.
        val typed = d.rendered().replaceFirst("Hi", "Hii")
        d = d.onInput(typed, TextRange(typed.length, typed.length))
        assertEquals(1, d.lines.count { it.image != null })
        assertEquals(3L, d.lines.first { it.image != null }.image?.attachmentId)
    }

    @Test fun image_isDroppedWhenItsPlaceholderIsDeleted() {
        var d = docOf("Hi", TextRange(2, 2)).insertImage(3L)
        val without = d.rendered().replace(IMAGE_PLACEHOLDER, "")
        d = d.onInput(without, TextRange(without.length, without.length))
        assertTrue(d.lines.none { it.image != null })
    }

    // ---------- export / import ----------

    @Test fun remapImageIds_pointsAtTheRestoredAttachments() {
        val d = docOf("Hi", TextRange(2, 2)).insertImage(3L)
        val rich = d.toRich()
        val remapped = RichText.remapImageIds(rich, mapOf(3L to 99L))
        assertEquals(99L, remapped.images.single().attachmentId)
        // A picture without a restored attachment is dropped, never dangling.
        val dropped = RichText.remapImageIds(rich, emptyMap())
        assertTrue(dropped.images.isEmpty())
    }
}
