package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryRestartContractTest {
    @Test
    fun matchingAnchorKeepsOffset() {
        assertEquals(480, validatedGalleryResumeOffset(480, 77L, 77L))
    }

    @Test
    fun staleAnchorFallsBackToStart() {
        assertEquals(0, validatedGalleryResumeOffset(480, 77L, 88L))
    }

    @Test
    fun endOfLibraryKeepsOffsetWithoutAnchor() {
        assertEquals(480, validatedGalleryResumeOffset(480, 0L, null))
    }
}
