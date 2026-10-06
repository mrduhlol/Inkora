package com.abhishek.inkora

import com.abhishek.inkora.domain.model.HwPoint
import com.abhishek.inkora.domain.model.HwStroke
import com.abhishek.inkora.domain.model.MAX_SCALE
import com.abhishek.inkora.domain.model.MAX_STROKES
import com.abhishek.inkora.domain.model.MIN_SCALE
import com.abhishek.inkora.domain.model.decodeHw
import com.abhishek.inkora.domain.model.encodeHw
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandwritingTest {

    private fun stroke(n: Int) = HwStroke(
        points = List(n) { i -> HwPoint(i.toFloat(), i.toFloat(), 0.8f) },
        colorArgb = 123,
        widthPx = 8f
    )

    @Test fun codec_roundTripsPressureAndFlags() {
        val strokes = listOf(stroke(3), HwStroke(listOf(HwPoint(1f, 1f)), 1, 4f, erase = true))
        val back = decodeHw(encodeHw(strokes))
        assertEquals(strokes, back)
    }

    @Test fun codec_corruptYieldsEmptyNeverCrash() {
        assertTrue(decodeHw("{nope").isEmpty())
        assertTrue(decodeHw("").isEmpty())
    }

    @Test fun codec_unknownFieldsIgnored() {
        // Forward compatibility: newer payloads decode on older code.
        assertTrue(decodeHw("""{"strokes":[],"future":1}""").isEmpty())
    }

    @Test fun canvasBounds_sane() {
        assertEquals(0.1f, MIN_SCALE)
        assertTrue(MAX_SCALE >= 8f)
        assertTrue(MAX_STROKES >= 1000)
    }
}
