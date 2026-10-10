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
    fun shortSwipeDoesNothing() {
        assertNull(gallerySwipeDirection(40f, 100f))
        assertNull(gallerySwipeDirection(-40f, 100f))
    }
}
