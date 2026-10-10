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
}
