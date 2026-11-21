package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryMediaGeometryTest {
    @Test fun portraitMediaUsesAvailableHeightWithoutSquareCrop() {
        assertEquals(600f, galleryFitHeightDp(360f, 600f, 1080, 1920), 0.01f)
    }
    @Test fun landscapeMediaPreservesOriginalRatio() {
        assertEquals(202.5f, galleryFitHeightDp(360f, 600f, 1920, 1080), 0.01f)
    }
    @Test fun squareImagesRemainSquare() {
        assertEquals(360f, galleryFitHeightDp(360f, 600f, 1080, 1080), 0.01f)
    }
    @Test fun veryWideMediaGetsAccessibleMinimumHeight() {
        assertEquals(180f, galleryFitHeightDp(360f, 600f, 4000, 500), 0.01f)
    }
    @Test fun missingDimensionsUseAvailableWindowRatherThanCropping() {
        assertEquals(600f, galleryFitHeightDp(360f, 600f, 0, 0), 0.01f)
    }

    @Test fun landscapeVideoIsLetterboxedVerticallyInsteadOfStretched() {
        val (sx, sy) = galleryVideoFitScales(400, 600, 1920, 1080)
        assertEquals(1f, sx, 0.0001f)
        assertEquals(0.375f, sy, 0.0001f)
    }
    @Test fun portraitVideoIsPillarboxedHorizontallyInsteadOfCropped() {
        val (sx, sy) = galleryVideoFitScales(400, 600, 1080, 1920)
        assertEquals(0.84375f, sx, 0.0001f)
        assertEquals(1f, sy, 0.0001f)
    }
    @Test fun invalidVideoMetadataDoesNotCreateInvalidTextureMatrix() {
        assertEquals(1f to 1f, galleryVideoFitScales(0, 600, 1920, 1080))
        assertEquals(1f to 1f, galleryVideoFitScales(400, 600, 0, 1080))
    }
}
