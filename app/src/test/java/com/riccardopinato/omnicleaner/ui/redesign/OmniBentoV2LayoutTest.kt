package com.riccardopinato.omnicleaner.ui.redesign

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OmniBentoV2LayoutTest {

    @Test fun positionalPaletteIsIndependentOfFeatureIdentity() {
        val tiles = (0 until 6).map { BentoV2Tile("feature-$it", 1, 1) }
        val first = requireNotNull(bentoV2Pack(tiles))
        val moved = requireNotNull(bentoV2Pack(tiles.reversed()))
        val firstIndices = bentoV2HarmonizedColorIndices(first)
        val movedIndices = bentoV2HarmonizedColorIndices(moved)
        val byPosition = firstIndices.entries.associate { (p, color) ->
            (p.row to p.column) to color
        }
        val afterByPosition = movedIndices.entries.associate { (p, color) ->
            (p.row to p.column) to color
        }
        assertEquals(byPosition, afterByPosition)
        assertTrue(
            "Moving a function to another slot must change its assigned color",
            firstIndices.entries.first { it.key.tile.id == "feature-0" }.value !=
                movedIndices.entries.first { it.key.tile.id == "feature-0" }.value,
        )
    }

    @Test fun neighborAwareColorsRemainStableWithoutRepeatsAcrossTouchingRectangles() {
        val placements = requireNotNull(bentoV2Pack(bentoV2Defaults()))
        val initial = bentoV2HarmonizedColorIndices(placements)
        val reverseInput = bentoV2HarmonizedColorIndices(placements.reversed())
        assertEquals(initial, reverseInput)
        assertEquals(placements.size, initial.size)
        assertTrue(initial.values.all { it in 0 until BENTO_V2_SWATCH_COUNT })
        for (a in placements) {
            for (b in placements) {
                if (a !== b && bentoV2SharedBorderUnits(a, b) > 0) {
                    assertTrue(
                        "Touching tiles must not share a color: ${a.tile.id} / ${b.tile.id}",
                        initial[a] != initial[b],
                    )
                }
            }
        }
    }

    @Test fun largeTileAdjacencyCountsSharedEdgesRatherThanOnlyListOrder() {
        val arrangement = requireNotNull(
            bentoV2Pack(listOf(
                BentoV2Tile("wide", 2, 2),
                BentoV2Tile("rightTop", 1, 1),
                BentoV2Tile("rightBottom", 1, 1),
            )),
        )
        val large = arrangement.first { it.tile.id == "wide" }
        assertEquals(1, bentoV2SharedBorderUnits(large, arrangement.first { it.tile.id == "rightTop" }))
        assertEquals(1, bentoV2SharedBorderUnits(large, arrangement.first { it.tile.id == "rightBottom" }))
        val assigned = bentoV2HarmonizedColorIndices(arrangement)
        assertTrue(assigned[large] != assigned[arrangement.first { it.tile.id == "rightTop" }])
        assertTrue(assigned[large] != assigned[arrangement.first { it.tile.id == "rightBottom" }])
    }

    @Test fun hiddenTileDoesNotClaimASwatchAndRepackingIsDeterministic() {
        val tiles = listOf(
            BentoV2Tile("a", 1, 1), BentoV2Tile("b", 1, 1),
            BentoV2Tile("c", 1, 1), BentoV2Tile("d", 1, 1),
            BentoV2Tile("e", 1, 1), BentoV2Tile("f", 1, 1, visible = false),
        )
        val packed = requireNotNull(bentoV2Pack(tiles))
        assertEquals(5, packed.size)
        assertTrue(packed.none { it.tile.id == "f" })
        assertEquals(bentoV2HarmonizedColorIndices(packed), bentoV2HarmonizedColorIndices(packed))
    }

    @Test fun accessibilityFontScalingExpandsCardRowsWithoutChangingGridWidth() {
        assertEquals(106f, bentoV2RowHeightDp(106f, 1.0f), 0.001f)
        assertEquals(106f, bentoV2RowHeightDp(106f, 1.1f), 0.001f)
        assertEquals(135f, bentoV2RowHeightDp(106f, 1.5f), 0.001f)
        assertEquals(180f, bentoV2RowHeightDp(106f, 2.0f), 0.001f)
        assertEquals(270f, bentoV2RowHeightDp(106f, 3.0f), 0.001f)
        assertEquals(0f, bentoV2RowHeightDp(-2f, 2.0f), 0.001f)
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
