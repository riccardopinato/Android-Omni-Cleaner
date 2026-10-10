package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryAccessPolicyTest {
    @Test
    fun android14SelectedPhotosIsPartialUsableAccess() {
        val health = evaluateGalleryAccess(
            sdkInt = 34,
            imagesGranted = false,
            videosGranted = false,
            selectedVisualGranted = true,
            legacyReadGranted = false,
        )

        assertEquals(GalleryAccessLevel.PARTIAL, health.level)
        assertTrue(health.hasAccess)
        assertTrue(health.canReadImages)
        assertTrue(health.canReadVideos)
    }

    @Test
    fun android14FullVisualPermissionsIsFullAccess() {
        val health = evaluateGalleryAccess(
            sdkInt = 35,
            imagesGranted = true,
            videosGranted = true,
            selectedVisualGranted = false,
            legacyReadGranted = false,
        )

        assertEquals(GalleryAccessLevel.FULL, health.level)
        assertTrue(health.canReadImages)
        assertTrue(health.canReadVideos)
    }

    @Test
    fun android13SingleMediaTypeIsPartialAccess() {
        val health = evaluateGalleryAccess(
            sdkInt = 33,
            imagesGranted = true,
            videosGranted = false,
            selectedVisualGranted = false,
            legacyReadGranted = false,
        )

        assertEquals(GalleryAccessLevel.PARTIAL, health.level)
        assertTrue(health.canReadImages)
        assertFalse(health.canReadVideos)
    }

    @Test
    fun legacyReadPermissionProvidesFullGalleryAccess() {
        val health = evaluateGalleryAccess(
            sdkInt = 32,
            imagesGranted = false,
            videosGranted = false,
            selectedVisualGranted = false,
            legacyReadGranted = true,
        )

        assertEquals(GalleryAccessLevel.FULL, health.level)
        assertTrue(health.hasAccess)
    }

    @Test
    fun noMediaPermissionMeansNoGalleryAccess() {
        val health = evaluateGalleryAccess(
            sdkInt = 35,
            imagesGranted = false,
            videosGranted = false,
            selectedVisualGranted = false,
            legacyReadGranted = false,
        )

        assertEquals(GalleryAccessLevel.NONE, health.level)
        assertFalse(health.hasAccess)
    }
}
