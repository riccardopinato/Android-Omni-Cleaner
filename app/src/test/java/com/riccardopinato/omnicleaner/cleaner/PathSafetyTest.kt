package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PathSafetyTest {
    @Test
    fun blocksSystemAndDeveloperRoots() {
        assertTrue(PathSafety.isDangerousPath(File("/system")))
        assertTrue(PathSafety.isDangerousPath(File("/storage/emulated/0")))
        assertTrue(PathSafety.isDangerousPath(File("/tmp/project/.git")))
    }

    @Test
    fun allowsOrdinaryChildFolderInsideRoot() {
        val root = createTempDir(prefix = "omni-root-")
        val child = File(root, "DownloadsToReview").apply { mkdirs() }
        try {
            assertTrue(PathSafety.isSafeFolderToDelete(child, root))
            assertFalse(PathSafety.isSafeFolderToDelete(root, root))
        } finally {
            root.deleteRecursively()
        }
    }
}
