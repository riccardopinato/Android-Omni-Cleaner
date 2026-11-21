package com.riccardopinato.omnicleaner.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OmniMediaQuickLookTest {
    @Test fun recognizesOriginalPhotoVideoAndAudioExtensions() {
        assertEquals(OmniQuickLookKind.PHOTO, omniQuickLookKind("picture.HEIC"))
        assertEquals(OmniQuickLookKind.PHOTO, omniQuickLookKind("image.avif"))
        assertEquals(OmniQuickLookKind.VIDEO, omniQuickLookKind("clip.MKV"))
        assertEquals(OmniQuickLookKind.VIDEO, omniQuickLookKind("movie.webm"))
        assertEquals(OmniQuickLookKind.AUDIO, omniQuickLookKind("song.flac"))
        assertEquals(OmniQuickLookKind.AUDIO, omniQuickLookKind("voice.OPUS"))
    }

    @Test fun providerMimeTypeTakesPrecedenceWhenExtensionIsMisleading() {
        assertEquals(OmniQuickLookKind.VIDEO, omniQuickLookKind("opaque.bin", "video/mp4"))
        assertEquals(OmniQuickLookKind.AUDIO, omniQuickLookKind("file.dat", "audio/ogg"))
        assertEquals(OmniQuickLookKind.PHOTO, omniQuickLookKind("unknown", "image/heic"))
    }

    @Test fun doesNotTryToRenderArbitraryDocumentsAsMedia() {
        assertNull(omniQuickLookKind("work.pdf"))
        assertNull(omniQuickLookKind("archive.zip"))
        assertNull(omniQuickLookKind("noextension"))
    }
}
