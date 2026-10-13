package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class PathSafetyTest {
    @Test
    fun blocksSystemAndDeveloperRoots() {
        assertTrue(PathSafety.isDangerousPath(File("/system")))
        assertTrue(PathSafety.isDangerousPath(File("/system/priv-app/SystemUI.apk")))
        assertTrue(PathSafety.isDangerousPath(File("/data/local/tmp/file.bin")))
        assertTrue(PathSafety.isDangerousPath(File("/storage/emulated/0")))
        assertTrue(
            PathSafety.isDangerousPath(
                File("/storage/emulated/0/Android/media/com.example.app/private.bin"),
            ),
        )
        assertTrue(PathSafety.isDangerousPath(File("/tmp/project/.git")))
    }

    @Test
    fun protectsWellKnownTopLevelUserFolders() {
        val root = createTempDirectory("omni-user-root-").toFile()
        val dcim = File(root, "DCIM").apply { mkdirs() }
        val nested = File(dcim, "Trip").apply { mkdirs() }
        try {
            assertFalse(PathSafety.isSafeFolderToDelete(dcim, root))
            assertTrue(PathSafety.isSafeFolderToDelete(nested, root))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun allowsOrdinaryChildFolderInsideRoot() {
        val root = createTempDirectory("omni-root-").toFile()
        val child = File(root, "DownloadsToReview").apply { mkdirs() }
        try {
            assertTrue(PathSafety.isSafeFolderToDelete(child, root))
            assertFalse(PathSafety.isSafeFolderToDelete(root, root))
        } finally {
            root.deleteRecursively()
        }
    }
    @Test
    fun appManagedPersistentTrashIsNeverAGeneralCleanupCandidate() {
        val root = createTempDirectory("omni-managed-").toFile()
        try {
            val managed = StorageAccess.safeTrashRoot(root).apply { mkdirs() }
            val payload = File(managed, "payload").apply { writeText("user-data") }

            assertTrue(PathSafety.isDangerousPath(managed))
            assertTrue(PathSafety.isDangerousPath(payload))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsAppManagedPersistentTrash() {
        val root = createTempDirectory("omni-managed-").toFile()
        try {
            val managed = File(root, StorageAccess.APP_MANAGED_DIRECTORY)
            val trash = File(managed, StorageAccess.SAFE_TRASH_DIRECTORY)
            trash.mkdirs()
            assertTrue(PathSafety.isDangerousPath(managed))
            assertTrue(PathSafety.isDangerousPath(trash))
        } finally {
            root.deleteRecursively()
        }
    }

}

