package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GallerySwipeDecisionTest {
    @Test
    fun fastSwipeSelectionKeepsAnimationAheadOfCommit() {
        assertTrue(GALLERY_EXIT_ANIMATION_MS in 100..180)
        assertTrue(GALLERY_SELECTION_COMMIT_DELAY_MS > GALLERY_EXIT_ANIMATION_MS)
        assertTrue(GALLERY_SELECTION_COMMIT_DELAY_MS <= 180)
    }

    @Test
    fun rightSwipeKeepsAndLeftSwipeTrashes() {
        assertEquals(SwipeDirection.KEEP, gallerySwipeDirection(150f, 100f))
        assertEquals(SwipeDirection.TRASH, gallerySwipeDirection(-150f, 100f))
    }

    @Test
    fun decisionAnimationAlwaysUsesTheSameSideAsSwipe() {
        assertEquals(-1, galleryDecisionExitSign(SwipeDirection.TRASH))
        assertEquals(1, galleryDecisionExitSign(SwipeDirection.KEEP))
        assertEquals(0, galleryDecisionExitSign(null))
    }

    @Test
    fun thresholdBoundaryUsesExpectedDirection() {
        assertEquals(SwipeDirection.TRASH, gallerySwipeDirection(-100f, 100f))
        assertEquals(SwipeDirection.KEEP, gallerySwipeDirection(100f, 100f))
        assertNull(gallerySwipeDirection(99f, 100f))
        assertNull(gallerySwipeDirection(-99f, 100f))
    }

    @Test
    fun videoScrubberClampsRealSeekAndDisplaysHours() {
        assertEquals(0, boundedVideoSeekPosition(-12, 90000))
        assertEquals(15000, boundedVideoSeekPosition(15000, 90000))
        assertEquals(90000, boundedVideoSeekPosition(100000, 90000))
        assertEquals(0, boundedVideoSeekPosition(500, -1))
        assertEquals("00:00", videoTimelineTimestamp(-10))
        assertEquals("01:13", videoTimelineTimestamp(73000))
        assertEquals("01:01:01", videoTimelineTimestamp(3661000))
        assertEquals("27:00:00", videoTimelineTimestamp(97200000))
    }

    @Test
    fun sessionSummaryUsesVerifiedPersistedAndRecentCounters() {
        val empty = gallerySessionSummary(GalleryUiState())
        assertEquals(0, empty.reviewedTotal)
        assertEquals(0, empty.selectedForAndroidTrash)
        assertNull(empty.recentKept)

        // The 50-entry bounded journal is NOT the lifetime kept count.
        val current = GalleryMediaItem(
            id = 42L,
            uri = android.net.Uri.parse("content://media/external/images/media/42"),
            name = "photo.jpg",
            sizeBytes = 100L,
            dateTaken = 0L,
            mimeType = "image/jpeg",
            album = null,
        )
        val summary = gallerySessionSummary(
            GalleryUiState(
                totalReviewedCount = 250,
                pendingTrashItems = listOf(current.copy(isSelectedForTrash = true)),
                history = listOf(
                    GallerySwipeHistoryItem(current, SwipeDirection.KEEP, 0),
                    GallerySwipeHistoryItem(current, SwipeDirection.TRASH, 1),
                ),
            ),
        )
        assertEquals(250, summary.reviewedTotal)
        assertEquals(1, summary.selectedForAndroidTrash)
        assertEquals(1, summary.recentKept)
    }

    @Test
    fun shortSwipeDoesNothing() {
        assertNull(gallerySwipeDirection(40f, 100f))
        assertNull(gallerySwipeDirection(-40f, 100f))
    }
}
