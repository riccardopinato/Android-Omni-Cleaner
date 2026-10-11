package com.riccardopinato.omnicleaner.cleaner

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SmartCategoryScannerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scanner = SmartCategoryScanner()
    private val now = 2_000_000_000_000L

    @Test
    fun classifiesReviewCandidatesWithoutDuplicatingFiles() {
        val root = temporaryFolder.newFolder("storage")
        val oldInstaller = file(root, "Download/old-build.apk")
        val screenshot = file(root, "Pictures/Screenshots/Screenshot_001.png")
        val archive = file(root, "Documents/export.zip")
        val recentDownload = file(root, "Download/recent.txt")

        val scan = scanner.classify(
            root = root,
            files = listOf(
                indexed(oldInstaller, 25L, now - days(120)),
                indexed(screenshot, 40L, now - days(5)),
                indexed(archive, 30L, now - days(10)),
                indexed(recentDownload, 50L, now - days(1)),
            ),
            nowMillis = now,
        )

        assertEquals(3, scan.items.size)

        val installerItem = scan.items.single { it.file == oldInstaller }
        assertTrue(SmartCategory.OLD_DOWNLOADS in installerItem.categories)
        assertTrue(SmartCategory.INSTALLERS in installerItem.categories)

        assertTrue(
            SmartCategory.SCREENSHOTS in
                scan.items.single { it.file == screenshot }.categories,
        )
        assertTrue(
            SmartCategory.ARCHIVES in
                scan.items.single { it.file == archive }.categories,
        )
        assertFalse(scan.items.any { it.file == recentDownload })

        assertEquals(95L, scan.totalUniqueBytes)
        assertEquals(25L, scan.bytesFor(SmartCategory.OLD_DOWNLOADS))
        assertEquals(25L, scan.bytesFor(SmartCategory.INSTALLERS))
    }

    @Test
    fun screenshotNameOutsideScreenshotFolderIsDetected() {
        val root = temporaryFolder.newFolder("storage")
        val screenshot = file(root, "DCIM/Camera/screen_shot_2026.jpg")

        val scan = scanner.classify(
            root = root,
            files = listOf(indexed(screenshot, 99L, now - days(2))),
            nowMillis = now,
        )

        assertEquals(1, scan.items.size)
        assertTrue(SmartCategory.SCREENSHOTS in scan.items.single().categories)
    }

    @Test
    fun zeroTimestampIsNotTreatedAsOldDownload() {
        val root = temporaryFolder.newFolder("storage")
        val unknownAge = file(root, "Download/unknown.bin")

        val scan = scanner.classify(
            root = root,
            files = listOf(indexed(unknownAge, 1L, 0L)),
            nowMillis = now,
        )

        assertTrue(scan.items.isEmpty())
    }

    @Test
    fun filesOutsideRootAreIgnored() {
        val root = temporaryFolder.newFolder("storage")
        val otherRoot = temporaryFolder.newFolder("other")
        val installer = file(otherRoot, "outside.apk")

        val scan = scanner.classify(
            root = root,
            files = listOf(indexed(installer, 100L, now - days(200))),
            nowMillis = now,
        )

        assertTrue(scan.items.isEmpty())
    }

    private fun indexed(file: File, bytes: Long, modified: Long): IndexedFile =
        IndexedFile(file = file, size = bytes, modified = modified)

    private fun file(root: File, relative: String): File =
        File(root, relative).also { candidate ->
            candidate.parentFile?.mkdirs()
            candidate.writeText("fixture")
        }

    private fun days(value: Long): Long = value * 24L * 60L * 60L * 1000L
}
