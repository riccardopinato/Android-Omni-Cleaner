package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Test

class OmniSourceCoverageTest {
    @Test fun deniedAndPartialSourcesNeverMasqueradeAsCompleteCoverage() {
        val result = OmniScanCoverageResult(
            sources = listOf(
                OmniScanCoverageSource(
                    "primary", OmniScanSourceKind.PRIMARY_FILES, "Internal",
                    OmniScanCoverageStatus.COMPLETE, 20, 0, "fully scanned",
                ),
                OmniScanCoverageSource(
                    "saf:a", OmniScanSourceKind.SAF_TREE, "SAF",
                    OmniScanCoverageStatus.PARTIAL, 400, 1, "capped",
                ),
                OmniScanCoverageSource(
                    "volume:b", OmniScanSourceKind.SECONDARY_VOLUME, "SD",
                    OmniScanCoverageStatus.DENIED, 0, 0, "permission denied",
                ),
                OmniScanCoverageSource(
                    "media:c", OmniScanSourceKind.MEDIASTORE, "Media",
                    OmniScanCoverageStatus.UNAVAILABLE, 0, 0, "not mounted",
                ),
            ),
            visibleResources = emptyList(),
        )
        assertEquals(2, result.accessibleSourceCount)
        assertEquals(2, result.excludedSourceCount)
    }
}
