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
}
