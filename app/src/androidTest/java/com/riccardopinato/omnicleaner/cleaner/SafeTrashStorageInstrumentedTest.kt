package com.riccardopinato.omnicleaner.cleaner

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeTrashStorageInstrumentedTest {
    /**
     * Synthetic Android runtime fixture only. Tests the real Java File API
     * under app-owned storage, not OEM shared-storage access or user data.
     */
    @Test
    fun fiftyMebibyteApkCanBeRestoredThenPurgedOnAndroidRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "omni-purge-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val trash = File(root, "safe-trash")
            val file = File(root, "fixture.apk")
            RandomAccessFile(file, "rw").use { raf ->
                raf.setLength(50L * 1024L * 1024L)
                raf.seek(25L * 1024L * 1024L)
                raf.write(byteArrayOf(16, 32, 64, 127))
            }
            val manager = SafeTrashManager.forTests(trash, root)
            assertNotNull(manager.moveToTrash(file))
            assertFalse(file.exists())
            assertEquals(1, manager.inspect().entries.size)

            val restored = manager.restore(manager.listEntries().single().id)
            assertEquals(SafeTrashRestoreStatus.RESTORED, restored.status)
            assertTrue(file.exists())
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(25L * 1024L * 1024L)
                assertEquals(16, raf.read())
                assertEquals(32, raf.read())
                assertEquals(64, raf.read())
                assertEquals(127, raf.read())
            }

            assertNotNull(manager.moveToTrash(file))
            val result = manager.purgeEntry(manager.listEntries().single().id)
            assertEquals(SafeTrashPurgeStatus.DELETED, result.status)
            assertEquals(50L * 1024L * 1024L, result.reclaimedBytes)
            assertFalse(file.exists())
            assertTrue(manager.listEntries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun safeTrashIsNotAppPrivateStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val trash = StorageAccess.safeTrashRoot().canonicalFile
        val privateFiles = context.filesDir.canonicalFile

        assertFalse(trash.path.startsWith(privateFiles.path))
        assertTrue(trash.path.contains(StorageAccess.APP_MANAGED_DIRECTORY))
        assertTrue(trash.path.endsWith(StorageAccess.SAFE_TRASH_DIRECTORY))
    }
}
