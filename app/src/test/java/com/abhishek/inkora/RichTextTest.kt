package com.abhishek.inkora

import com.abhishek.inkora.domain.model.BlockKind
import com.abhishek.inkora.domain.model.RichBlock
import com.abhishek.inkora.domain.model.RichContent
import com.abhishek.inkora.domain.model.RichText
import com.abhishek.inkora.domain.model.SpanKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextTest {

    @Test fun bold_appliesOnlyToSelection() {
        val c = RichText.toggleSpan(RichContent("Hello World"), 6, 11, SpanKind.BOLD)
        assertEquals(1, c.spans.size)
        assertEquals(6, c.spans[0].start)
        assertEquals(11, c.spans[0].end)
        assertEquals("Hello World", c.text) // no ** inserted
    }

    @Test fun bold_togglesOffWhenFullyCovered() {
        val c0 = RichText.toggleSpan(RichContent("Hello World"), 6, 11, SpanKind.BOLD)
        val c1 = RichText.toggleSpan(c0, 6, 11, SpanKind.BOLD)
        assertTrue(c1.spans.isEmpty())
        assertEquals("Hello World", c1.text)
    }

    @Test fun partialOverlap_expandsAndMerges() {
        val c0 = RichText.toggleSpan(RichContent("Hello World"), 0, 5, SpanKind.BOLD)
        val c1 = RichText.toggleSpan(c0, 3, 8, SpanKind.BOLD)
        assertEquals(1, c1.spans.size)
        assertEquals(0, c1.spans[0].start)
        assertEquals(8, c1.spans[0].end)
    }

    @Test fun unbold_middle_splitsSpan() {
        val c0 = RichText.toggleSpan(RichContent("Hello World"), 0, 11, SpanKind.BOLD)
        val c1 = RichText.toggleSpan(c0, 3, 8, SpanKind.BOLD)
        assertEquals(2, c1.spans.size)
    }

    @Test fun collapsedSelection_isNoop() {
        val c = RichText.toggleSpan(RichContent("Hello"), 2, 2, SpanKind.ITALIC)
        assertTrue(c.spans.isEmpty())
    }

    @Test fun italic_underline_strike_doNotInsertMarkers() {
        var c = RichContent("Hello")
        c = RichText.toggleSpan(c, 0, 5, SpanKind.ITALIC)
        c = RichText.toggleSpan(c, 0, 5, SpanKind.UNDERLINE)
        c = RichText.toggleSpan(c, 0, 5, SpanKind.STRIKE)
        assertEquals("Hello", c.text)
        assertEquals(3, c.spans.size)
    }

    @Test fun bullet_selectionAcrossLines_setsEachLine() {
        val text = "Apple\nBanana\nOrange"
        val c = RichText.setBlock(RichContent(text), 0..2, BlockKind.BULLET)
        assertEquals(3, c.blocks.size)
        assertTrue(c.blocks.all { it.kind == BlockKind.BULLET })
        assertEquals("Apple\nBanana\nOrange", c.text)
    }

    @Test fun bullet_toggleOff_revertsToParagraph() {
        val c0 = RichText.setBlock(RichContent("A\nB"), 0..1, BlockKind.BULLET)
        val c1 = RichText.setBlock(c0, 0..1, BlockKind.BULLET)
        assertTrue(c1.blocks.isEmpty())
    }

    @Test fun numbered_producesSeparateLines() {
        val c = RichText.setBlock(RichContent("Apple\nBanana"), 0..1, BlockKind.NUMBERED)
        assertEquals("1. Apple\n2. Banana", RichText.plain(c))
    }

    @Test fun checklist_toggleChecked() {
        val c0 = RichText.setBlock(RichContent("Study"), 0..0, BlockKind.CHECK)
        assertEquals("☐ Study", RichText.plain(c0))
        val c1 = RichText.toggleCheck(c0, 0)
        assertEquals("☑ Study", RichText.plain(c1))
        assertTrue(RichText.plain(c1).contains("☑"))
    }

    @Test fun enter_emptyBulletLine_exitsList() {
        val c = RichContent("", emptyList(), listOf(com.abhishek.inkora.domain.model.RichBlock(0, BlockKind.BULLET)))
        val next = RichText.enterBlocks(c, 0, lineTextIsBlank = true, newLineCount = 1)
        assertTrue(next.isEmpty())
    }

    @Test fun migrate_boldAndBullets() {
        val c = RichText.migrate("**hello** world\n- Apple", "md-v1")
        assertEquals("hello world\nApple", c.text)
        assertEquals(1, c.spans.count { it.kind == SpanKind.BOLD })
        assertEquals(BlockKind.BULLET, c.blocks.first().kind)
    }

    @Test fun migrate_underlineStrikeItalic_checklist() {
        val c = RichText.migrate("<u>Hi</u> _there_ ~~gone~~ __under__\n- [ ] Task\n- [x] Done", "md-v1")
        assertTrue(c.spans.any { it.kind == SpanKind.UNDERLINE })
        assertTrue(c.spans.any { it.kind == SpanKind.ITALIC })
        assertTrue(c.spans.any { it.kind == SpanKind.STRIKE })
        assertEquals("Hi there gone under\nTask\nDone", c.text)
        assertEquals(2, c.blocks.count { it.kind == BlockKind.CHECK })
        assertTrue(c.blocks.any { it.checked })
    }

    @Test fun migrate_neverLosesText() {
        val raw = "weird **unclosed and <u>broken"
        val c = RichText.migrate(raw, "md-v1")
        assertTrue(c.text.contains("unclosed"))
        assertTrue(c.text.contains("broken"))
        assertTrue(!c.text.contains("**") && !c.text.contains("<u>"))
    }

    @Test fun preview_neverShowsSyntax() {
        val legacy = "**Bold** _it_ <u>u</u> ~~s~~\n- [ ] item"
        val p = RichText.previewText(legacy, "md-v1")
        assertTrue(!p.contains("**") && !p.contains("<u>") && !p.contains("~~") && !p.contains("[ ]"))
        val rich = RichText.encode(RichText.migrate(legacy, "md-v1"))
        val p2 = RichText.previewText(rich, "rich-v1")
        assertTrue(!p2.contains("**") && !p2.contains("[ ]"))
    }

    @Test fun encode_decode_roundTrip() {
        val c = RichText.toggleSpan(RichContent("Hi"), 0, 2, SpanKind.BOLD)
        val back = RichText.decodeOrNull(RichText.encode(c))
        assertEquals(c, back)
    }

    @Test fun `corruptRich fallsBackToStrippedText`() {
        val c = RichText.migrate("{not json **hi**!", "rich-v1")
        assertTrue(c.text.contains("hi"))
        assertTrue(!c.text.contains("**"))
    }

    @Test fun displayTitle_prefersStoredTitle() {
        assertEquals("Mine", RichText.displayTitle("Mine", "Other body", "md-v1"))
    }

    @Test fun displayTitle_derivesFromFirstContentLine() {
        assertEquals("Shopping", RichText.displayTitle("", "\nShopping\n- eggs", "md-v1"))
    }

    @Test fun displayTitle_stripsMarkersAndCapsLength() {
        val t = RichText.displayTitle("", "**Bold** intro", "md-v1")
        assertEquals("Bold intro", t)
        assertEquals("", RichText.displayTitle("", "   \n  ", "md-v1"))
    }

    @Test fun structureBlocks_toggleAndPlain() {
        var c = RichContent("Title\nBody")
        c = RichText.setBlock(c, 0..0, BlockKind.HEADING1)
        c = RichText.setBlock(c, 1..1, BlockKind.QUOTE)
        assertEquals(BlockKind.HEADING1, RichText.blockAt(c, 0).kind)
        val p = RichText.plain(c)
        assertTrue(p.contains("Title") && p.contains("Body"))
        assertTrue(!p.contains("#"))
        // Toggle off reverts to paragraph.
        c = RichText.setBlock(c, 0..0, BlockKind.HEADING1)
        assertEquals(BlockKind.PARAGRAPH, RichText.blockAt(c, 0).kind)
    }

    @Test fun divider_plainHasNoDashesSyntax() {
        val c = RichContent("", emptyList(), listOf(RichBlock(0, BlockKind.DIVIDER)))
        val p = RichText.plain(c)
        assertTrue(!p.contains("---"))
    }

    @Test fun normalizeUrl_addsSchemeAndRejectsGarbage() {
        assertEquals("https://example.com", RichText.normalizeUrl("example.com"))
        assertEquals("https://example.com/a", RichText.normalizeUrl("https://example.com/a"))
        assertEquals(null, RichText.normalizeUrl(""))
        assertEquals(null, RichText.normalizeUrl("not a url"))
        assertEquals(null, RichText.normalizeUrl("localhost"))
    }

    @Test fun link_setRemoveAndLookup() {
        var c = RichContent("OpenAI rules")
        c = RichText.setLink(c, 0, 6, "https://openai.com")
        assertEquals("OpenAI rules", c.text) // text untouched
        assertEquals("https://openai.com", RichText.linkAt(c, 3)?.url)
        assertEquals(null, RichText.linkAt(c, 8))
        c = RichText.removeLinksIn(c, 0, 6)
        assertTrue(c.links.isEmpty())
    }

    @Test fun link_overlappingReplaced() {
        var c = RichContent("Hello World")
        c = RichText.setLink(c, 0, 5, "https://a.com")
        c = RichText.setLink(c, 3, 8, "https://b.com")
        assertEquals(1, c.links.size)
        assertEquals("https://b.com", c.links.single().url)
    }

    @Test fun table_cellsRoundTrip() {
        val row = RichText.buildTableRow(listOf("Physics", "85"))
        assertEquals(listOf("Physics", "85"), RichText.tableCells(row))
        assertEquals(listOf(""), RichText.tableCells(""))
    }

    @Test fun table_groupSpansConsecutiveRows() {
        val c = RichContent(
            "Intro\nA │ 1\nB │ 2\nOutro",
            emptyList(),
            listOf(RichBlock(1, BlockKind.TABLE), RichBlock(2, BlockKind.TABLE))
        )
        assertEquals(1..2, RichText.tableGroup(c, 1))
        assertEquals(1..2, RichText.tableGroup(c, 2))
        assertEquals(null, RichText.tableGroup(c, 0))
        assertEquals(2, RichText.tableColCount(c, 1..2))
    }

    @Test fun code_enterContinuesAndBlankExits() {
        val c = RichContent("print(1)", emptyList(), listOf(RichBlock(0, BlockKind.CODE)))
        val continued = RichText.enterBlocks(c, 0, lineTextIsBlank = false, newLineCount = 2)
        assertEquals(BlockKind.CODE, continued.first { it.line == 1 }.kind)
        val exited = RichText.enterBlocks(c, 0, lineTextIsBlank = true, newLineCount = 2)
        assertTrue(exited.none { it.line == 0 })
    }
}
