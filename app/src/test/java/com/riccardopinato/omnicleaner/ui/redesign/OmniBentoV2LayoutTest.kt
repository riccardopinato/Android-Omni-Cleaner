package com.riccardopinato.omnicleaner.ui.redesign

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OmniBentoV2LayoutTest {

    @Test fun popFreshPaletteHasExactUserApprovedNineSwatches() {
        val palette = OmniColorBentoV2Palette
        assertEquals(androidx.compose.ui.graphics.Color(0xFF3C55E8), palette.tileColor("storage"))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFC9F25D), palette.tileColor("duplicates"))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFF7A73), palette.tileColor("large"))
        assertEquals(androidx.compose.ui.graphics.Color(0xFF5EDBD5), palette.tileColor("gallery"))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFC4A2FF), palette.tileColor("privacy"))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFFAE58), palette.tileColor("trash"))
        assertEquals(androidx.compose.ui.graphics.Color(0xFF87DEAA), palette.tileColor("folders"))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFFE066), palette.tileColor("review"))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFF83C2), palette.tileColor("pro"))
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF7F7F4), palette.tileColor("intelligence"))
    }

    @Test fun defaultsHaveNoDuplicatesAndFitThreeColumns() {
        val entries = bentoV2Defaults()
        assertEquals(16, entries.size)
        assertEquals(entries.size, entries.map { it.id }.distinct().size)
        assertTrue(entries.all { it.visible })
        assertNotNull(bentoV2Pack(entries))
    }

    @Test fun resizePreviewSnapsToCellSizesAndNeverExceedsGrid() {
        val tile = BentoV2Tile("example", 1, 1)
        assertEquals(1 to 1, bentoV2ResizeFromDrag(tile, 0f, 0f, 100f))
        assertEquals(2 to 1, bentoV2ResizeFromDrag(tile, 65f, 5f, 100f))
        assertEquals(3 to 2, bentoV2ResizeFromDrag(tile, 200f, 100f, 100f))
        assertEquals(1 to 1, bentoV2ResizeFromDrag(tile, -500f, -500f, 100f))
        assertEquals(3 to 2, bentoV2ResizeFromDrag(tile, 1000f, 1000f, 100f))
        assertEquals(1 to 1, bentoV2ResizeFromDrag(tile, 1000f, 1000f, 0f))
    }

    @Test fun everyApprovedSizeIsSupported() {
        assertEquals(
            setOf(1 to 1, 2 to 1, 1 to 2, 2 to 2, 3 to 1, 3 to 2),
            BENTO_V2_SIZES,
        )
        BENTO_V2_SIZES.forEach { (w, h) ->
            assertTrue(BentoV2Tile("test", w, h).sizeIsValid())
        }
        assertFalse(BentoV2Tile("test", 3, 3).sizeIsValid())
    }

    @Test fun rejectsUnfillableInteriorInsteadOfSilentlyResizing() {
        // A single two-row tile cannot fit in only one row.
        assertNull(bentoV2Pack(listOf(BentoV2Tile("alone", 1, 2))))
        assertNull(bentoV2Pack(listOf(BentoV2Tile("a", 1, 1), BentoV2Tile("a", 1, 1))))
    }

    @Test fun placementsNeverOverlapAndLeaveOnlyTrailingCellsFree() {
        val packed = requireNotNull(bentoV2Pack(bentoV2Defaults()))
        val occupied = mutableSetOf<Pair<Int, Int>>()
        for (position in packed) {
            assertTrue(position.column >= 0)
            assertTrue(position.column + position.tile.columns <= 3)
            for (dy in 0 until position.tile.rows) for (dx in 0 until position.tile.columns) {
                assertTrue(occupied.add((position.row + dy) to (position.column + dx)))
            }
        }
        val last = packed.maxOf { it.row + it.tile.rows }
        val cells = (0 until last).flatMap { y -> (0 until 3).map { x -> y to x } }
        val firstHole = cells.indexOfFirst { it !in occupied }
        if (firstHole >= 0) {
            assertTrue(cells.drop(firstHole).all { it !in occupied })
        }
    }
}
