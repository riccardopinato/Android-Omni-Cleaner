package com.riccardopinato.omnicleaner.cleaner

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.createTempDirectory

class ThumbnailFileCleanupTest {
    @Test
    fun onlyUnchangedVerifiedThumbnailMovesToReversibleTrash() = runBlocking {
        val root = createTempDirectory("omni-thumbnails-").toFile()
        try {
            val cache = root.resolve("DCIM/.thumbnails").apply { mkdirs() }
            val stable = cache.resolve("stable.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val changed = cache.resolve("changed.jpg").apply { writeBytes(byteArrayOf(4, 5, 6)) }
            val trash = SafeTrashManager.forTests(root.resolve(".OmniCleaner/SafeTrash"), root)
            val cleaner = ThumbnailFileCleanup(trash)
            val snapshot = cleaner.inventory(cache)
            changed.writeBytes(byteArrayOf(9, 9, 9, 9))

            val result = cleaner.clean(snapshot)

            assertEquals(1, result.removed)
            assertEquals(1, result.rejected)
            assertEquals(3L, result.removedBytes)
            assertFalse(stable.exists())
            assertTrue(changed.exists())
            assertTrue(cache.isDirectory)
            assertEquals(1, trash.listEntries().size)
            assertEquals("stable.jpg", trash.listEntries().single().originalName)

            val restored = trash.restore(trash.listEntries().single().id)
            assertEquals(SafeTrashRestoreStatus.RESTORED, restored.status)
            assertTrue(stable.exists())
            assertEquals(listOf<Byte>(1, 2, 3), stable.readBytes().toList())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun noAuthorizedTrashManagerMeansNoDeletion() = runBlocking {
        val root = createTempDirectory("omni-thumbnail-closed-").toFile()
        try {
            val cache = root.resolve("DCIM/.thumbnails").apply { mkdirs() }
            val thumb = cache.resolve("unique.jpg").apply { writeText("keep") }
            val cleaner = ThumbnailFileCleanup()
            val snapshot = cleaner.inventory(cache)
            val result = cleaner.clean(snapshot)
            assertEquals(0, result.removed)
            assertEquals(1, result.rejected)
            assertEquals("keep", thumb.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun arbitraryFoldersCannotUseThumbnailMediaBypass() = runBlocking {
        val root = createTempDirectory("omni-thumbnail-unsafe-").toFile()
        try {
            val unsafe = root.resolve("Pictures/Personal").apply { mkdirs() }
            val photo = unsafe.resolve("portrait.jpg").apply { writeText("not a cache") }
            val trash = SafeTrashManager.forTests(root.resolve(".OmniCleaner/SafeTrash"), root)
            val cleaner = ThumbnailFileCleanup(trash)
            val result = cleaner.clean(cleaner.inventory(unsafe))
            assertEquals(0, result.removed)
            assertTrue(photo.isFile)
            assertTrue(trash.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
