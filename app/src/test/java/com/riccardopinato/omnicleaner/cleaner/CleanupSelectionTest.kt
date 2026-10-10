package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class CleanupSelectionTest {
    @Test
    fun duplicateProtectionKeepsOldestDeterministically() {
        val root = createTempDirectory("omni-keeper-").toFile()
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
                sizeBytes = newest.length(),
                files = listOf(
                    DuplicateFile(
                        file = newest,
                        name = newest.name,
                        path = newest.absolutePath,
                        displayPath = newest.absolutePath,
                        sizeBytes = newest.length(),
                        lastModified = newest.lastModified(),
                        extension = newest.extension,
                    ),
                    DuplicateFile(
                        file = oldest,
                        name = oldest.name,
                        path = oldest.absolutePath,
                        displayPath = oldest.absolutePath,
                        sizeBytes = oldest.length(),
                        lastModified = oldest.lastModified(),
                        extension = oldest.extension,
                    ),
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
        val root = createTempDirectory("omni-directory-").toFile()
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
        val root = createTempDirectory("omni-unique-").toFile()
        try {
            val file = File(root, "file.txt").apply { writeText("x") }
            val alias = File(root, "./file.txt")
            assertEquals(1, CleanupSelection.uniqueEntries(listOf(file, alias)).size)
        } finally {
            root.deleteRecursively()
        }
    }
}
