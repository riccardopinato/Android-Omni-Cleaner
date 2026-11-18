package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class StorageIntelligenceScaleTest {
    @Test
    fun thousandsOfIndexedFilesRemainBoundedAndSearchCountsStayExact() {
        val root = createTempDirectory("omni-intelligence-scale-").toFile()
        try {
            val inventory = FileInventory(
                files = (1..3000).map { n ->
                    IndexedFile(
                        file = File(root, "Folder${n % 30}/sample-${n.toString().padStart(5, '0')}.pdf"),
                        size = (n % 100).toLong() * 1024L,
                        modified = 1_700_000_000_000L,
                    )
                },
                unreadable = 0,
                excludedDirectories = 1,
            )
            val report = StorageIntelligenceAnalyzer().fromInventory(root, inventory)
            assertEquals(3000, report.indexedFileCount)
            assertEquals(30, report.folders.size)
            val page = report.search(text = ".PDF", limit = 10_000)
            assertEquals(3000, page.matchCount)
            assertEquals(200, page.files.size) // bounded list, not 3000 Compose rows
            val scoped = report.search(folder = "Folder9", limit = 20)
            assertEquals(100, scoped.matchCount)
            assertEquals(20, scoped.files.size)
            assertTrue(scoped.files.all { it.relativePath.startsWith("Folder9/") })
            assertEquals(0, report.search(text = "notpresent").matchCount)
        } finally {
            root.deleteRecursively()
        }
    }
}
