package com.riccardopinato.omnicleaner.exif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifGroupContractTest {
    @Test
    fun donorGroupsAndTitlesStayStable() {
        assertEquals(
            listOf("Posizione GPS", "Date e orari EXIF", "Dispositivo e autore", "Commenti EXIF"),
            ExifGroup.entries.map { it.title },
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
