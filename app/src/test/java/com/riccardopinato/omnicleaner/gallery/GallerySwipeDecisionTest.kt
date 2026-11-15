package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GallerySwipeDecisionTest {
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
