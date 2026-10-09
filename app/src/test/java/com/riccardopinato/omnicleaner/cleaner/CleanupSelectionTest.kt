package com.riccardopinato.omnicleaner.cleaner

import com.riccardopinato.omnicleaner.model.DuplicateFile
import com.riccardopinato.omnicleaner.model.DuplicateGroup
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupSelectionTest {
    @Test
    fun duplicateProtection_neverLeavesGroupWithoutKeeper() {
        val root = Files.createTempDirectory("omni-protect").toFile()
        try {
            val a = root.resolve("a").apply { writeText("same") }
            val b = root.resolve("b").apply { writeText("same") }
            val group = DuplicateGroup(
                hash = "x",
                sizeBytes = a.length(),
                files = listOf(
                    DuplicateFile(a, isSelected = true, lastModified = 1),
                    DuplicateFile(b, isSelected = true, lastModified = 2),
                ),
            )

            val result = CleanupSelection.protectDuplicates(listOf(group))

            assertEquals(1, result.keepers.size)
            assertTrue(result.guardians.size <= 1)
        } finally {
            root.deleteRecursively()
        }
    }
}
