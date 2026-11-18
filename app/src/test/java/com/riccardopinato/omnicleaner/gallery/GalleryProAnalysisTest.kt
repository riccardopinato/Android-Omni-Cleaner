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
