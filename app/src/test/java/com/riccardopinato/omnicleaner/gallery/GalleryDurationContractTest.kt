package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

/** Factory Spec Lite: FR-001, AC-001..AC-004. No device, URI, or provider. */
class GalleryDurationContractTest {
    @Test
    fun originalHourLongRegressionRendersHoursNotTotalMinutes() {
        // Old GalleryMediaItem.formattedDuration calculation: 3_723_000 -> "62:03".
        assertEquals("01:02:03", videoTimelineTimestamp(3_723_000L))
    }

    @Test
    fun normalSubHourDurationKeepsExistingMinuteSecondFormat() {
        assertEquals("00:00", videoTimelineTimestamp(0L))
        assertEquals("01:13", videoTimelineTimestamp(73_000L))
        assertEquals("59:59", videoTimelineTimestamp(3_599_000L))
    }

    @Test
    fun longDurationsDoNotOverflowIntAndPreserveHours() {
        assertEquals("596:31:23", videoTimelineTimestamp(2_147_483_000L))
        assertEquals("597:00:00", videoTimelineTimestamp(2_149_200_000L))
        assertEquals("27:00:00", videoTimelineTimestamp(97_200_000L))
    }

    @Test
    fun originalIntScrubberContractsStillUseSameSharedFormatter() {
        assertEquals("01:01:01", videoTimelineTimestamp(3_661_000))
        assertEquals("00:00", videoTimelineTimestamp(-10))
        assertEquals("01:01:01", videoTimelineTimestamp(3_661_000L))
    }
}
