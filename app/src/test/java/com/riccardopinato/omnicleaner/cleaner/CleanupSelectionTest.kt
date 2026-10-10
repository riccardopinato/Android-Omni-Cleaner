package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CleanupSelectionTest {
    @Test
    fun duplicateProtectionKeepsOldestDeterministically() {
        val root = createTempDir(prefix = "omni-keeper-")
        try {
            val oldest = File(root, "oldest.bin").apply {
                writeText("same")
                setLastModified(100L)
            }
            val newest = File(root, "newest.bin").apply {
                writeText("same")
                setLastModified(200L)
            }
            val group = DuplicateGroup(
                hash = "hash",
                files = listOf(
                    DuplicateFile(newest, newest.length(), newest.lastModified()),
                    DuplicateFile(oldest, oldest.length(), oldest.lastModified()),
                ),
            )

            val protection = CleanupSelection.protectDuplicates(listOf(group))

            assertEquals(setOf(oldest.canonicalFile), protection.keepers.map { it.canonicalFile }.toSet())
            assertEquals(oldest.canonicalFile, protection.guardians[newest.canonicalPath]?.canonicalFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun containsKeeperUnderDirectory() {
        val root = createTempDir(prefix = "omni-directory-")
        try {
            val directory = File(root, "Pictures").apply { mkdirs() }
            val keeper = File(directory, "keep.jpg").apply { writeText("x") }

            assertTrue(CleanupSelection.containsKeeper(directory, setOf(keeper)))
            assertFalse(CleanupSelection.containsKeeper(File(root, "Other"), setOf(keeper)))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun uniqueEntriesUsesCanonicalPaths() {
        val root = createTempDir(prefix = "omni-unique-")
        try {
            val file = File(root, "file.txt").apply { writeText("x") }
            val alias = File(root, "./file.txt")
            assertEquals(1, CleanupSelection.uniqueEntries(listOf(file, alias)).size)
        } finally {
            root.deleteRecursively()
        }
    }
}
