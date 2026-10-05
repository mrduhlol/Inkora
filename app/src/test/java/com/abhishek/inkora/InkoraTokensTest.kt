package com.abhishek.inkora

import androidx.compose.ui.graphics.Color
import com.abhishek.inkora.ui.theme.darken
import com.abhishek.inkora.ui.theme.foldUndersideFor
import com.abhishek.inkora.ui.theme.lighten
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InkoraTokensTest {

    @Test fun darken_movesTowardBlack() {
        val c = darken(Color.White, 0.5f)
        assertEquals(0.5f, c.red, 0.01f)
        assertEquals(1f, c.alpha, 0f)
    }

    @Test fun lighten_movesTowardWhite() {
        val c = lighten(Color.Black, 0.5f)
        assertEquals(0.5f, c.red, 0.01f)
    }

    @Test fun factors_clamped() {
        assertEquals(Color.White, darken(Color.White, -1f))
        assertEquals(Color.Black, darken(Color.White, 2f))
    }

    @Test fun fold_differsFromPaperOnBothThemes() {
        val lightFold = foldUndersideFor(Color(0xFFFAF3E3))
        val darkFold = foldUndersideFor(Color(0xFF1E1D1B))
        assertTrue(lightFold != Color(0xFFFAF3E3))
        assertTrue(darkFold != Color(0xFF1E1D1B))
    }
}
