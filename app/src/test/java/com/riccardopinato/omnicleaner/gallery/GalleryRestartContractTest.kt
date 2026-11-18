package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun durableUndoRoundTripsKeepAndTrash() {
        listOf(SwipeDirection.KEEP, SwipeDirection.TRASH).forEach { direction ->
            val journal = PersistedGalleryUndo(
                mediaId = 7728L,
                direction = direction,
                sourceOffsetBefore = 511,
            )
            assertEquals(journal, decodePersistedGalleryUndo(encodePersistedGalleryUndo(journal)))
        }
    }

    @Test
    fun malformedUndoReferenceFailsClosed() {
        listOf(null, "", "nope", "12\tKEEP\t-1", "0\tTRASH\t3",
            "41\tERASE\t7", "8\tTRASH\tNaN", "5\tKEEP\t9\textra")
            .forEach { raw -> assertNull(decodePersistedGalleryUndo(raw)) }
    }

    @Test
    fun undoUsesSafeSourceAnchor() {
        val previous = PersistedGalleryUndo(91L, SwipeDirection.TRASH, 200)
        assertEquals(200, validatedGalleryResumeOffset(
            previous.sourceOffsetBefore,
            previous.mediaId,
            previous.mediaId,
        ))
        assertEquals(0, validatedGalleryResumeOffset(
            previous.sourceOffsetBefore,
            previous.mediaId,
            92L,
        ))
    }

    @Test
    fun endOfLibraryKeepsOffsetWithoutAnchor() {
        assertEquals(480, validatedGalleryResumeOffset(480, 0L, null))
    }
}
