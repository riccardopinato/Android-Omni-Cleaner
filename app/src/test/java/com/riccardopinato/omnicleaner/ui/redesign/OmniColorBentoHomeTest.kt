package com.riccardopinato.omnicleaner.ui.redesign

import com.riccardopinato.omnicleaner.cleaner.StorageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Truthful home metrics: no synthetic figures before a real scan. */
class OmniColorBentoHomeTest {
    @Test fun noStorageMeansNoPercent() {
        assertNull(bentoPercent(null))
        assertNull(bentoPercent(StorageInfo(totalBytes = 0, freeBytes = 0)))
    }

    @Test fun storagePercentIsMeasuredAndBounded() {
        assertEquals(50, bentoPercent(StorageInfo(totalBytes = 2000, freeBytes = 1000)))
        assertEquals(0, bentoPercent(StorageInfo(totalBytes = 2000, freeBytes = 3000)))
        assertEquals(100, bentoPercent(StorageInfo(totalBytes = 2000, freeBytes = -20)))
    }

    @Test fun noScanCannotMasqueradeAsZeroResults() {
        assertEquals("—", bentoVerifiedCount(scanned = false, count = 200))
        assertEquals("0", bentoVerifiedCount(scanned = true, count = 0))
        assertEquals("28", bentoVerifiedCount(scanned = true, count = 28))
    }

    @Test fun byteFormatRejectsInventedNegativeValues() {
        assertEquals("—", bentoByteLabel(-1))
        assertEquals("0 B", bentoByteLabel(0))
        assertEquals("1.0 KB", bentoByteLabel(1024))
    }
}
