package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryModelContractTest {
    @Test
    fun donorFilterAndSortContractsStayStable() {
        assertEquals(listOf("Tutti", "Solo Foto", "Solo Video"), GalleryMediaFilter.entries.map { it.label })
        assertEquals(
            listOf("Più recenti", "Meno recenti", "Più grandi", "Più piccoli", "Nome A-Z", "Per Album"),
            GallerySortOrder.entries.map { it.label },
        )
        assertEquals(listOf(SwipeDirection.KEEP, SwipeDirection.TRASH), SwipeDirection.entries)
    }
}
