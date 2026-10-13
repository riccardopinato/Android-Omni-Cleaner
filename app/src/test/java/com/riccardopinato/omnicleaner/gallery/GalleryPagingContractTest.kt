package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryPagingContractTest {
    @Test
    fun pageBoundaryUsesOneLookaheadItem() {
        val requestedPageSize = 120
        val returned = List(requestedPageSize + 1) { it }

        assertTrue(returned.size > requestedPageSize)
        assertEquals(requestedPageSize, returned.take(requestedPageSize).size)
    }

    @Test
    fun finalPageHasNoMoreWhenLookaheadIsMissing() {
        val requestedPageSize = 120
        val returned = List(27) { it }

        assertFalse(returned.size > requestedPageSize)
    }
    @Test
    fun reviewWindowCompactionKeepsRecentReviewedItemsAndUnreviewedTail() {
        val items = (0 until 400).toList()

        val compacted = compactGalleryReviewWindow(
            items = items,
            currentIndex = 300,
            maxReviewedItemsInMemory = 240,
            retainReviewedItems = 60,
        )

        assertEquals((0 until 240).toList(), compacted.removedPrefix)
        assertEquals((240 until 400).toList(), compacted.items)
        assertEquals(60, compacted.currentIndex)
    }

    @Test
    fun reviewWindowDoesNotCompactBelowThreshold() {
        val items = (0 until 200).toList()

        val compacted = compactGalleryReviewWindow(
            items = items,
            currentIndex = 180,
            maxReviewedItemsInMemory = 240,
            retainReviewedItems = 60,
        )

        assertTrue(compacted.removedPrefix.isEmpty())
        assertEquals(items, compacted.items)
        assertEquals(180, compacted.currentIndex)
    }

}
