package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class StorageCleanupSelectionTest {
    @Test
    fun duplicateSelectionNeverReturnsEveryCopyWhenAllWereSelected() {
        val root = createTempDirectory("omni-keeper-").toFile()
        try {
            val oldest = File(root, "a.bin").apply {
                writeText("same")
                setLastModified(100L)
            }
            val newer = File(root, "b.bin").apply {
                writeText("same")
                setLastModified(200L)
            }
            val group = DuplicateGroup(
                hash = "hash",
                sizeBytes = 4L,
                files = listOf(
                    DuplicateFile(oldest, oldest.name, oldest.path, oldest.path, 4L, 100L, "bin"),
                    DuplicateFile(newer, newer.name, newer.path, newer.path, 4L, 200L, "bin"),
                ),
            )

            val selectedPaths = group.files.mapTo(linkedSetOf()) { it.path }
            val protected = protectForTest(listOf(group), selectedPaths)

            assertEquals(1, protected.size)
            assertFalse(oldest in protected)
            assertTrue(newer in protected)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun protectForTest(
        groups: List<DuplicateGroup>,
        selectedPaths: Set<String>,
    ): List<File> {
        val selected = ArrayList<File>()
        for (group in groups) {
            val selectedInGroup = group.files.filter { it.path in selectedPaths }
            val keeper = if (selectedInGroup.size == group.files.size) {
                group.files.minWithOrNull(
                    compareBy<DuplicateFile> { it.lastModified }.thenBy { it.path },
                )
            } else null
            selected += selectedInGroup
                .filterNot { keeper != null && it.path == keeper.path }
                .map { it.file }
        }
        return CleanupSelection.uniqueEntries(selected)
    }
}
