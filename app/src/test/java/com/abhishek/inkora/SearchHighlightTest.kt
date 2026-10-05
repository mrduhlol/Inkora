package com.abhishek.inkora

import com.abhishek.inkora.ui.components.highlightRanges
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHighlightTest {

    @Test fun blankQuery_yieldsNothing() {
        assertTrue(highlightRanges("Hello World", "").isEmpty())
        assertTrue(highlightRanges("Hello World", "   ").isEmpty())
        assertTrue(highlightRanges("", "hi").isEmpty())
    }

    @Test fun caseInsensitive_singleMatch() {
        assertEquals(listOf(6 until 11), highlightRanges("Hello World", "world"))
    }

    @Test fun multipleNonOverlapping_capped() {
        val text = "aa ".repeat(20)
        val ranges = highlightRanges(text, "aa")
        assertEquals(8, ranges.size)
        assertEquals(0 until 2, ranges.first())
    }

    @Test fun overlapping_matchesAdvancePastMatch() {
        // "aa" in "aaa": first match 0..2, next search starts at 2 → no second match.
        assertEquals(listOf(0 until 2), highlightRanges("aaa", "aa"))
    }

    @Test fun unicodeSafe() {
        assertEquals(listOf(0 until 2), highlightRanges("Semiconductor…", "semi"))
    }
}
