package com.abhishek.inkora

import com.abhishek.inkora.domain.model.AccentColor
import com.abhishek.inkora.ui.theme.accentSeed
import com.abhishek.inkora.ui.theme.inkoraDarkScheme
import com.abhishek.inkora.ui.theme.inkoraLightScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeTest {

    @Test fun accent_changesSchemeContainers() {
        val orange = inkoraLightScheme(accentSeed(AccentColor.ORANGE))
        val purple = inkoraLightScheme(accentSeed(AccentColor.PURPLE))
        assertNotEquals(purple.primary, orange.primary)
        assertNotEquals(purple.primaryContainer, orange.primaryContainer)
        // FAB/switch/dialog slots follow the seed too.
        assertNotEquals(purple.secondaryContainer, orange.secondaryContainer)
        assertNotEquals(purple.secondary, orange.secondary)
    }

    @Test fun amoled_usesTrueBlack_darkUsesNearBlack() {
        val seed = accentSeed(AccentColor.TEAL)
        val amoled = inkoraDarkScheme(seed, amoled = true)
        val dark = inkoraDarkScheme(seed, amoled = false)
        assertEquals(0xFF000000.toInt(), amoled.background.value.toInt())
        assertNotEquals(amoled.background, dark.background)
    }

    @Test fun allAccents_produceDistinctPrimaries() {
        val primaries = AccentColor.entries.map { inkoraLightScheme(accentSeed(it)).primary }.toSet()
        assertEquals(AccentColor.entries.size, primaries.size)
    }
}
