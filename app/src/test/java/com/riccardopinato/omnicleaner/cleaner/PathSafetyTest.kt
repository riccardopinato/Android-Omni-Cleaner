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
        assertTrue(PathSafety.isDangerousPath(File("/storage/emulated/0")))
        assertTrue(PathSafety.isDangerousPath(File("/tmp/project/.git")))
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
