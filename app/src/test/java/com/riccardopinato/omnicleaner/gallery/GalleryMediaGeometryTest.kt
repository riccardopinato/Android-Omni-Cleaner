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
}
