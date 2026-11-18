package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryVideoPlayerPolicyTest {
    @Test
    fun positionFractionStaysWithinRange() {
        assertEquals(0f, galleryPlaybackFraction(1000, 0), 0.0001f)
        assertEquals(0f, galleryPlaybackFraction(-200, 1000), 0.0001f)
        assertEquals(0.25f, galleryPlaybackFraction(250, 1000), 0.0001f)
        assertEquals(1f, galleryPlaybackFraction(3000, 1000), 0.0001f)
    }

    @Test
    fun seekAndSkipDoNotOverflowDuration() {
        assertEquals(0, gallerySeekPositionMs(-0.5f, 120000))
        assertEquals(60000, gallerySeekPositionMs(0.5f, 120000))
        assertEquals(120000, gallerySeekPositionMs(3f, 120000))
        assertEquals(0, gallerySeekPositionMs(Float.NaN, 120000))
        assertEquals(0, gallerySkipPositionMs(1000, -10000, 120000))
        assertEquals(120000, gallerySkipPositionMs(119000, 10000, 120000))
        assertEquals(0, gallerySkipPositionMs(10, 10000, 0))
        assertEquals(120000, gallerySkipPositionMs(Int.MAX_VALUE, Int.MAX_VALUE, 120000))
    }

    @Test
    fun elapsedTimeHandlesHoursAndInvalidInput() {
        assertEquals("00:00", galleryTimeLabel(-999))
        assertEquals("01:09", galleryTimeLabel(69500))
        assertEquals("1:02:03", galleryTimeLabel(3723000))
    }
}
