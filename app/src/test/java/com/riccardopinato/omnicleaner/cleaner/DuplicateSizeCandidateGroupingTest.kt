package com.riccardopinato.omnicleaner.cleaner

import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateSizeCandidateGroupingTest {
    // Reference algorithm before allocation optimization, not a deletion verdict.
    private fun baseline(files: List<IndexedFile>): List<List<IndexedFile>> =
        uniquePhysicalFiles(files).asSequence()
            .filter { it.size > 0L }
            .groupBy { it.size }.values
            .filter { it.size > 1 }
            .sortedWith(
                compareByDescending<List<IndexedFile>> { group ->
                    val size = group.first().size
                    val extra = (group.size - 1).toLong()
                    if (size > Long.MAX_VALUE / extra) Long.MAX_VALUE else size * extra
                }.thenBy { group -> group.minOf { it.file.absolutePath } },
            )

    private fun snapshot(grouped: List<List<IndexedFile>>): List<Pair<Long, List<String>>> =
        grouped.map { group ->
            group.first().size to group.map { it.file.absolutePath }
        }

    private fun virtualFile(index: Int, size: Long): IndexedFile =
        IndexedFile(File("/synthetic/factory-" + index.toString().padStart(7, '0') + ".bin"), size, 1L)

    @Test
    fun mostlyUniqueSizesRetainExactPreviousGroupsAndPriority() {
        val files = (0 until 30_000).map { virtualFile(it, it.toLong() + 1L) }.toMutableList()
        files += virtualFile(30_000, 27_000L)
        files += virtualFile(30_001, 17_000L)
        files += virtualFile(30_002, 27_000L)
        files += virtualFile(30_003, 0L)
        files += virtualFile(30_004, -1L)

        val expected = snapshot(baseline(files))
        val actual = snapshot(duplicateCandidateSizeGroups(files))
        assertEquals(expected, actual)
        assertEquals(2, actual.size)
        assertEquals(3, actual.first().second.size)
        assertTrue(actual.first().first > actual.last().first)
    }

    @Test
    fun seededMultisizeAndPhysicalAliasesMatchBaseline() {
        val random = Random(20261119)
        val files = (0 until 5_000).map { i ->
            virtualFile(i, random.nextInt(0, 1_000).toLong())
        }.toMutableList()
        val shared = PhysicalFileIdentity(deviceId = 12L, inode = 554L)
        files += IndexedFile(File("/synthetic/alias-a.bin"), 4096L, 1L, physicalIdentity = shared)
        files += IndexedFile(File("/synthetic/alias-b.bin"), 4096L, 1L, physicalIdentity = shared)
        files += virtualFile(6000, 4096L)
        assertEquals(snapshot(baseline(files)), snapshot(duplicateCandidateSizeGroups(files)))
        assertEquals(2, duplicateCandidateSizeGroups(files).first { it.first().size == 4096L }.size)
    }

    @Test
    fun veryLargePotentialWasteSaturatesWithoutOverflow() {
        val huge = Long.MAX_VALUE - 1L
        val files = listOf(
            virtualFile(0, huge), virtualFile(1, huge), virtualFile(2, huge),
            virtualFile(3, 20L), virtualFile(4, 20L),
            virtualFile(5, 0L),
        )
        assertEquals(snapshot(baseline(files)), snapshot(duplicateCandidateSizeGroups(files)))
        assertEquals(huge, duplicateCandidateSizeGroups(files).first().first().size)
    }

    @Test
    fun noDuplicatesCreatesNoCandidateGroups() {
        val onlyUnique = (0 until 5_000).map { virtualFile(it, it.toLong() + 1L) }
        assertTrue(duplicateCandidateSizeGroups(onlyUnique).isEmpty())
        assertTrue(duplicateCandidateSizeGroups(emptyList()).isEmpty())
    }
}
