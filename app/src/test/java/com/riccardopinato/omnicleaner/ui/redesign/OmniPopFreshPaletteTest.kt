package com.riccardopinato.omnicleaner.ui.redesign

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/** The user-selected nine-color set is an exact token contract, not an approximation. */
class OmniPopFreshPaletteTest {
    @Test fun nineDistinctApprovedSwatchesAreMappedToBentoTiles() {
        val expected = mapOf(
            "storage" to Color(0xFF3C55E8), // Electric Blue
            "duplicates" to Color(0xFFC9F25D), // Lime Pop
            "large" to Color(0xFFFF7A73), // Coral Pop
            "gallery" to Color(0xFF5EDBD5), // Aqua Fresh
            "privacy" to Color(0xFFC4A2FF), // Soft Violet
            "trash" to Color(0xFFFFAE58), // Tangerine
            "folders" to Color(0xFF87DEAA), // Spring Green
            "review" to Color(0xFFFFE066), // Sunshine
            "pro" to Color(0xFFFF83C2), // Bubblegum
        )
        assertEquals(9, expected.values.distinct().size)
        expected.forEach { (id, color) ->
            assertEquals("Unexpected palette for $id", color, OmniColorBentoV2Palette.tileColor(id))
        }
    }

    @Test fun electricBlueUsesWhiteInkAndLightSwatchesUseDarkInk() {
        assertEquals(Color.White, OmniColorBentoV2Palette.tileForeground("storage"))
        assertEquals(Color(0xFF192033), OmniColorBentoV2Palette.tileForeground("duplicates"))
        assertEquals(Color(0xFF192033), OmniColorBentoV2Palette.tileForeground("privacy"))
    }

    @Test fun unknownTileUsesNeutralSurface() {
        assertEquals(Color(0xFFF7F7F4), OmniColorBentoV2Palette.tileColor("unknown"))
    }
}
