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
    fun shortSwipeDoesNothing() {
        assertNull(gallerySwipeDirection(40f, 100f))
        assertNull(gallerySwipeDirection(-40f, 100f))
    }
}
