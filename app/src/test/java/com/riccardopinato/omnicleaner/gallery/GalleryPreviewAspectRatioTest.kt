package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryPreviewAspectRatioTest {
    @Test
    fun portraitAndLandscapeFramesPreserveProportions() {
        assertEquals(0.75f, previewFrameAspectRatio(3000, 4000), 0.001f)
        assertEquals(1.5f, previewFrameAspectRatio(6000, 4000), 0.001f)
        assertEquals(1.0f, previewFrameAspectRatio(1024, 1024), 0.001f)
    }

    @Test
    fun invalidOrExtremeMetadataDoesNotBreakReviewLayout() {
        assertEquals(1f, previewFrameAspectRatio(0, 100), 0.001f)
        assertEquals(1f, previewFrameAspectRatio(400, 0), 0.001f)
        assertEquals(0.5f, previewFrameAspectRatio(100, 1000), 0.001f)
        assertEquals(2f, previewFrameAspectRatio(1000, 100), 0.001f)
    }
}
