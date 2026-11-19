package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryProAnalysisTest {
    @Test
    fun videoFiltersUseRealMetadataAndNeverInferReclaimableSpace() {
        val videos = listOf(
            GalleryVideoCandidate(1, "trip_4k.mp4", 350L * 1024L * 1024L, 400_000L, 3840, 2160, "Trip"),
            GalleryVideoCandidate(2, "short.mp4", 30L * 1024L * 1024L, 10_000L, 1920, 1080, "Trip"),
            GalleryVideoCandidate(3, "long.mov", 70L * 1024L * 1024L, 320_000L, 1080, 1920, "Family"),
        )
        assertEquals(3, filterGalleryVideos(videos, GalleryVideoReviewFilter.ALL).totalMatching)
        val large = filterGalleryVideos(videos, GalleryVideoReviewFilter.LARGE_100_MB)
        assertEquals(1, large.totalMatching)
        assertEquals(1L, large.results.single().id)
        assertEquals(2, filterGalleryVideos(videos, GalleryVideoReviewFilter.LONG_5_MIN).totalMatching)
        assertEquals(1, filterGalleryVideos(videos, GalleryVideoReviewFilter.UHD_4K).totalMatching)
        assertEquals(2, filterGalleryVideos(videos, GalleryVideoReviewFilter.ALL, text = "trIP").totalMatching)
        assertEquals(1, filterGalleryVideos(videos, GalleryVideoReviewFilter.ALL, text = ".mov").totalMatching)
        assertEquals(3, filterGalleryVideos(videos, GalleryVideoReviewFilter.ALL, maxVisible = 1).totalMatching)
        assertEquals(1, filterGalleryVideos(videos, GalleryVideoReviewFilter.ALL, maxVisible = 1).results.size)
        assertEquals(350L * 1024L * 1024L, large.bytesMatching)
    }

    @Test
    fun sizeRangeUsesInclusiveRealBytesAndCombinesWithPresetAndSearch() {
        val mib = 1024L * 1024L
        val videos = listOf(
            GalleryVideoCandidate(1L, "holiday.mp4", 100L * mib, 480_000L, 3840, 2160, "Trip"),
            GalleryVideoCandidate(2L, "long_trip.mp4", 150L * mib, 350_000L, 1920, 1080, "Trip"),
            GalleryVideoCandidate(3L, "family.mov", 200L * mib, 30_000L, 1920, 1080, "Family"),
            GalleryVideoCandidate(4L, "large.mp4", 900L * mib, 480_000L, 3840, 2160, "Trip"),
        )
        val range = GalleryVideoSizeRange(100L * mib, 200L * mib)
        val result = filterGalleryVideos(videos, GalleryVideoReviewFilter.ALL, sizeRange = range)
        assertEquals(3, result.totalMatching)
        assertEquals(listOf(3L, 2L, 1L), result.results.map { it.id })
        assertEquals(450L * mib, result.bytesMatching)

        val combined = filterGalleryVideos(
            videos,
            GalleryVideoReviewFilter.LONG_5_MIN,
            text = "trip",
            sizeRange = range,
        )
        assertEquals(listOf(2L), combined.results.map { it.id })
        assertEquals(150L * mib, combined.bytesMatching)
        val capped = filterGalleryVideos(
            videos,
            GalleryVideoReviewFilter.ALL,
            maxVisible = 1,
            sizeRange = range,
        )
        assertEquals(3, capped.totalMatching)
        assertEquals(1, capped.results.size)
        assertEquals(450L * mib, capped.bytesMatching)
    }

    @Test
    fun sizeRangeDefaultIsInactiveAndRejectsInvalidBounds() {
        val single = listOf(
            GalleryVideoCandidate(9L, "video.mp4", 0L, 0L, 0, 0, null),
        )
        assertEquals(1, filterGalleryVideos(single, GalleryVideoReviewFilter.ALL).totalMatching)
        assertEquals(
            1,
            filterGalleryVideos(
                single,
                GalleryVideoReviewFilter.ALL,
                sizeRange = GalleryVideoSizeRange(0L, 0L),
            ).totalMatching,
        )
        assertEquals(
            0,
            filterGalleryVideos(
                single,
                GalleryVideoReviewFilter.ALL,
                sizeRange = GalleryVideoSizeRange(1L, Long.MAX_VALUE),
            ).totalMatching,
        )
        try {
            GalleryVideoSizeRange(2L, 1L)
            org.junit.Assert.fail("Inverted size bounds should be rejected")
        } catch (_: IllegalArgumentException) {
            // Correct
        }
        try {
            GalleryVideoSizeRange(-1L, 1L)
            org.junit.Assert.fail("Negative size bounds should be rejected")
        } catch (_: IllegalArgumentException) {
            // Correct
        }
    }

    @Test
    fun perceptualGroupingRequiresBothTemporalAndVisualSimilarity() {
        val base = 1_700_000_000_000L
        val photos = listOf(
            GalleryPhotoSignature(1, base, 0L),
            GalleryPhotoSignature(2, base + 1_000L, 1L),
            GalleryPhotoSignature(3, base + 10_000L, -1L),
            GalleryPhotoSignature(4, base + 120_000L, 0L),
            GalleryPhotoSignature(5, 0L, 0L),
        )
        val groups = groupSimilarGalleryPhotos(photos)
        assertEquals(1, groups.size)
        assertEquals(listOf(1L, 2L), groups.single().photoIds)
        assertTrue(groupSimilarGalleryPhotos(emptyList()).isEmpty())
        assertTrue(groupSimilarGalleryPhotos(photos, maxHammingDistance = 0).isEmpty())
    }

    @Test
    fun boundedGroupingDoesNotConflateFarApartOrMissingTimestamps() {
        val base = 1_700_000_000_000L
        val distinct = (1..200).map {
            GalleryPhotoSignature(it.toLong(), base + it * 100_000L, 0L)
        }
        assertTrue(groupSimilarGalleryPhotos(distinct).isEmpty())
        assertTrue(groupSimilarGalleryPhotos(distinct, maxPhotos = 0).isEmpty())
        assertTrue(groupSimilarGalleryPhotos(listOf(
            GalleryPhotoSignature(10, 0L, 0L), GalleryPhotoSignature(11, 0L, 0L),
        )).isEmpty())
    }
}
