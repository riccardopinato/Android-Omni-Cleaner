package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** User-selected Pop Fresh colors; a tile's ID must not own its color. */
class OmniPopFreshPaletteTest {
    @Test fun exactlyNineApprovedSwatchesAreAvailableForPositions() {
        val expected = listOf(
            Color(0xFFC9F25D), // Lime Pop
            Color(0xFF3C55E8), // Electric Blue
            Color(0xFFFFAE58), // Tangerine
            Color(0xFFC4A2FF), // Soft Violet
            Color(0xFFFFE066), // Sunshine
            Color(0xFF5EDBD5), // Aqua Fresh
            Color(0xFFFF7A73), // Coral Pop
            Color(0xFF87DEAA), // Spring Green
            Color(0xFFFF83C2), // Bubblegum
        )
        assertEquals(BENTO_V2_SWATCH_COUNT, expected.size)
        assertEquals(expected.size, expected.distinct().size)
        assertEquals(expected, OmniColorBentoV2Palette.positionSwatches)
    }

    @Test fun positionalElectricBlueUsesWhiteInkAndLightSwatchesUseDarkInk() {
        val palette = OmniColorBentoV2Palette
        assertEquals(Color.White, palette.tileForeground(palette.blue))
        assertEquals(Color(0xFF192033), palette.tileForeground(palette.green))
        assertEquals(Color(0xFF192033), palette.tileForeground(palette.violet))
        assertTrue(palette.positionSwatches.count { palette.tileForeground(it) == Color.White } == 1)
    }

    @Test fun neutralBackgroundIsNotAnAdditionalCardSwatch() {
        val palette = OmniColorBentoV2Palette
        assertEquals(Color(0xFFF7F7F4), palette.cream)
        assertTrue(palette.cream !in palette.positionSwatches)
    }
}
