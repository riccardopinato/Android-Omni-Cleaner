package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryModelContractTest {
    @Test
    fun donorFilterAndSortSemanticContractsStayStable() {
        assertEquals(
            listOf("ALL", "IMAGES", "VIDEOS"),
            GalleryMediaFilter.entries.map { it.name },
        )
        assertEquals(
            listOf("DATE_DESC", "DATE_ASC", "SIZE_DESC", "SIZE_ASC", "NAME_ASC", "ALBUM"),
            GallerySortOrder.entries.map { it.name },
        )
        assertEquals(listOf(SwipeDirection.KEEP, SwipeDirection.TRASH), SwipeDirection.entries)
    }
}
