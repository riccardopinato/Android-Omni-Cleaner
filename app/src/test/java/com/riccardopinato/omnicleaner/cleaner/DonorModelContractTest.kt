package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Test

class DonorModelContractTest {
    @Test
    fun folderEnumsMatchRecoveredApkContract() {
        assertEquals(
            listOf(FolderStatus.COMPLETELY_EMPTY, FolderStatus.BELOW_THRESHOLD),
            FolderStatus.entries,
        )
        assertEquals(
            listOf(
                FolderSubTab.EMPTY_FOLDERS,
                FolderSubTab.APP_RESIDUES,
                FolderSubTab.THUMBNAIL_CACHE,
            ),
            FolderSubTab.entries,
        )
    }

    @Test
    fun thresholdUnitsMatchRecoveredApkContract() {
        assertEquals(1L, ThresholdUnit.BYTES.multiplier)
        assertEquals(1024L, ThresholdUnit.KB.multiplier)
        assertEquals(1024L * 1024L, ThresholdUnit.MB.multiplier)
    }
}
