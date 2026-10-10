package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class FileInventoryPolicyTest {
    @Test
    fun ignoresDonorExcludedNamesAndHiddenDirectories() {
        val root = createTempDirectory("omni-policy-").toFile()
        try {
            assertTrue(FileInventoryPolicy.shouldIgnore(root, File(root, "cache")))
            assertTrue(FileInventoryPolicy.shouldIgnore(root, File(root, "lost+found")))
            assertTrue(FileInventoryPolicy.shouldIgnore(root, File(root, ".private")))
            assertFalse(FileInventoryPolicy.shouldIgnore(root, File(root, "Download")))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun keepsAndroidMediaButExcludesProtectedAndroidSubtrees() {
        val root = createTempDirectory("omni-android-policy-").toFile()
        try {
            assertFalse(FileInventoryPolicy.shouldIgnore(root, File(root, "Android/media")))
            assertTrue(FileInventoryPolicy.shouldIgnore(root, File(root, "Android/data")))
            assertTrue(FileInventoryPolicy.shouldIgnore(root, File(root, "Android/obb")))
        } finally {
            root.deleteRecursively()
        }
    }
}
