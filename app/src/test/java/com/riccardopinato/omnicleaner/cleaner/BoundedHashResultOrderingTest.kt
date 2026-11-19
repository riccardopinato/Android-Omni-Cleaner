package com.riccardopinato.omnicleaner.cleaner

import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The index-slot optimization must preserve deterministic results despite
 * unpredictable worker completion order; it is not a physical speedup claim.
 */
class BoundedHashResultOrderingTest {
    @Test fun reverseWorkerCompletionPreservesOrderAndSparseIndices() = runBlocking {
        val input = (0 until 240).map { index ->
            IndexedFile(File("/synthetic/factory-${index}.bin"), 1L, 1L)
        }
        var peak = 0
        repeat(3) {
            val results = boundedHashFiles(
                items = input,
                parallelism = 4,
                onInFlightChanged = { active -> synchronized(this) { peak = maxOf(peak, active) } },
            ) { item ->
                val index = item.file.name.substringAfter("factory-").substringBefore(".bin").toInt()
                delay(((240 - index) % 11).toLong())
                if (index % 7 == 0) null else "sha-$index"
            }
            val expected = (0 until input.size).filter { it % 7 != 0 }
            assertEquals(expected, results.map { it.index })
            assertEquals(expected.map { "sha-$it" }, results.map { it.hash })
            assertEquals(expected.map { input[it] }, results.map { it.item })
        }
        assertTrue("bounded at 4 worker slots", peak in 1..4)
    }

    @Test fun emptyAndAllNullBatchesStayEmpty() = runBlocking {
        assertTrue(boundedHashFiles(emptyList(), 4) { "unexpected" }.isEmpty())
        val input = (0..15).map { IndexedFile(File("/synthetic/empty-$it"), 1L, 1L) }
        assertTrue(boundedHashFiles(input, 4) { null }.isEmpty())
    }
}
