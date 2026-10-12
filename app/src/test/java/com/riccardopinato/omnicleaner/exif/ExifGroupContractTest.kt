package com.riccardopinato.omnicleaner.exif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifGroupContractTest {
    @Test
    fun donorGroupsStayStableWhileLabelsLiveInResources() {
        assertEquals(
            listOf("LOCATION", "DATES", "DEVICE", "COMMENTS"),
            ExifGroup.entries.map { it.name },
        )
        assertTrue("GPSLatitude" in ExifGroup.LOCATION.tags)
        assertTrue("DateTimeOriginal" in ExifGroup.DATES.tags)
        assertTrue("LensSerialNumber" in ExifGroup.DEVICE.tags)
        assertTrue("UserComment" in ExifGroup.COMMENTS.tags)
    }

    @Test
    fun selectedTagsIsUnionOfSelectedGroups() {
        val tags = ExifGroup.selectedTags(setOf(ExifGroup.LOCATION, ExifGroup.COMMENTS))
        assertTrue("GPSLongitude" in tags)
        assertTrue("ImageDescription" in tags)
        assertTrue("Make" !in tags)
    }
}
