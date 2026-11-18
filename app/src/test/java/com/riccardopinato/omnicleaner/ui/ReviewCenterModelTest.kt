package com.riccardopinato.omnicleaner.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewCenterModelTest {
    @Test
    fun localFileOverlapIsCountedOnceAndMediaStaySeparate() {
        val model = buildReviewCenterSnapshot(
            largeFiles = listOf(
                ReviewQueueEntry("/storage/A.mp4", "A", 700L),
                ReviewQueueEntry("/storage/B.zip", "B", 300L),
            ),
            duplicateFiles = listOf(
                ReviewQueueEntry("/storage/A.mp4", "A", 700L),
                ReviewQueueEntry("/storage/C.txt", "C", 100L),
            ),
            androidMedia = listOf(
                ReviewQueueEntry("media:1", "A", 700L),
                ReviewQueueEntry("media:1", "A", 700L),
                ReviewQueueEntry("media:2", "D", 800L),
            ),
            selectedFolders = 2,
            staleSelections = 1,
        )
        assertEquals(3, model.filesystemEntries.size)
        assertEquals(1_100L, model.filesystemIndexedBytes)
        assertEquals(2, model.mediaEntries.size)
        assertEquals(1_500L, model.mediaQueuedBytes)
        assertEquals(2, model.selectedFolders)
        assertEquals(1, model.staleSelections)
        // Intentionally no combined "reclaimable" result in the API.
        assertFalse(model.javaClass.methods.any { it.name == "getReclaimedBytes" })
    }

    @Test
    fun blankQueueAndNegativeSizesProduceNoFakeSavings() {
        val model = buildReviewCenterSnapshot(
            largeFiles = listOf(ReviewQueueEntry("item", "broken", -10L)),
            duplicateFiles = emptyList(),
            androidMedia = emptyList(),
            selectedFolders = -5,
            staleSelections = -2,
        )
        assertEquals(0L, model.filesystemIndexedBytes)
        assertEquals(0L, model.mediaQueuedBytes)
        assertEquals(0, model.selectedFolders)
        assertEquals(0, model.staleSelections)
        assertTrue(model.mediaEntries.isEmpty())
    }
}
