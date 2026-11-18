package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class StorageIntelligenceTest {
    @Test
    fun mapsIndexedBytesWithoutTreatingOverlappingSuggestionsAsSavings() {
        val root = createTempDirectory("omni-intelligence-").toFile()
        try {
            val now = 1_800_000_000_000L
            val old = now - 100L * 24L * 60L * 60L * 1000L
            val inventory = FileInventory(
                files = listOf(
                    IndexedFile(File(root, "Download/old.apk"), 5_000_000, old),
                    IndexedFile(File(root, "Download/recent.zip"), 3_000_000, now),
                    IndexedFile(File(root, "Pictures/Screenshots/screenshot_1.png"), 1_000_000, now),
                    IndexedFile(File(root, "notes.txt"), 500, now),
                    // Read-only model must reject a sibling outside the scanned root.
                    IndexedFile(File(root.parentFile, root.name + "-outside/file.apk"), 7_000_000, old),
                ),
                unreadable = 2,
                excludedDirectories = 3,
            )

            val report = StorageIntelligenceAnalyzer().fromInventory(root, inventory, now)
            assertEquals(4, report.indexedFileCount)
            assertEquals(9_000_500L, report.indexedBytes)
            assertEquals(2, report.unreadable)
            assertEquals(3, report.excludedDirectories)
            assertEquals(8_000_000L, report.folders.single { it.name == "Download" }.bytes)
            assertEquals(2, report.folders.single { it.name == "Download" }.fileCount)
            assertEquals(500L, report.folders.single { it.name.isEmpty() }.bytes)
            assertEquals(1, report.suggestions.single {
                it.category == SmartCategory.OLD_DOWNLOADS
            }.fileCount)
            assertEquals(1, report.suggestions.single {
                it.category == SmartCategory.INSTALLERS
            }.fileCount)
            assertTrue(report.suggestions.sumOf { it.bytes } > report.indexedBytes / 2)

            val apk = report.search(text = ".APK")
            assertEquals(1, apk.matchCount)
            assertEquals("Download/old.apk", apk.files.single().relativePath)
            val downloads = report.search(folder = "Download", limit = 1)
            assertEquals(2, downloads.matchCount)
            assertEquals(1, downloads.files.size)
            assertEquals("old.apk", downloads.files.single().name)
            assertEquals(1, report.search(minimumBytes = 5_000_000L).matchCount)
            assertEquals(0, report.search(folder = "Wrong", text = ".apk").matchCount)
            assertEquals(0, report.search(limit = -5).files.size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun emptyAndUnavailableInventoryProducesNoFakeReclaimSuggestions() {
        val root = createTempDirectory("omni-intelligence-empty-").toFile()
        try {
            val report = StorageIntelligenceAnalyzer().fromInventory(
                root,
                FileInventory(files = emptyList(), unreadable = 4, excludedDirectories = 5),
            )
            assertTrue(report.folders.isEmpty())
            assertTrue(report.suggestions.isEmpty())
            assertEquals(0L, report.indexedBytes)
            assertEquals(0, report.search(text = "anything").matchCount)
            assertFalse(report.search().files.isNotEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
