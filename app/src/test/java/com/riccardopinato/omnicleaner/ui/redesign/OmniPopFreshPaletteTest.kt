package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Approved Soft Pastel Home: nine calm positional tones, not feature colors. */
class OmniPopFreshPaletteTest {
    @Test fun nineSoftPastelSwatchesFollowPositionalIdentity() {
        val expected = listOf(
            Color(0xFFDCEEC9), // Pear
            Color(0xFFD5E5FA), // Powder blue
            Color(0xFFF9E5D3), // Peach
            Color(0xFFE9E2F6), // Lavender
            Color(0xFFF8EFD2), // Butter
            Color(0xFFD9EFF1), // Misty aqua
            Color(0xFFF6DFDA), // Gentle coral
            Color(0xFFDBEEE4), // Mint
            Color(0xFFF6E0EB), // Powder pink
        )
        assertEquals(BENTO_V2_SWATCH_COUNT, expected.size)
        assertEquals(expected.size, expected.distinct().size)
        assertEquals(expected, OmniColorBentoV2Palette.positionSwatches)
    }

    @Test fun everyPastelUsesDarkAccessibleInk() {
        val palette = OmniColorBentoV2Palette
        for (swatch in palette.positionSwatches) {
            assertEquals(Color(0xFF263440), palette.tileForeground(swatch))
        }
        assertEquals(Color(0xFFF8F9F6), palette.cream)
        assertTrue(palette.cream !in palette.positionSwatches)
    }

    @Test fun smallTilesAreSofterWithoutChangingThePositionalSwatch() {
        val palette = OmniColorBentoV2Palette
        val base = palette.positionSwatches[1]
        val hero = palette.surfaceForPosition(base, columns = 2, rows = 2)
        val medium = palette.surfaceForPosition(base, columns = 2, rows = 1)
        val small = palette.surfaceForPosition(base, columns = 1, rows = 1)
        assertEquals(base, hero)
        assertNotEquals(hero, medium)
        assertNotEquals(medium, small)
        assertEquals(base, palette.positionSwatches[1])
    }

    @Test fun originalNativeIllustrationsCoverPriorityModules() {
        for (id in listOf("storage", "duplicates", "large", "gallery", "privacy", "trash", "folders")) {
            assertTrue(id, bentoHasSoftIllustration(id))
        }
    }
}
