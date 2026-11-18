package com.riccardopinato.omnicleaner.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaTrashBatchPolicyTest {
    @Test
    fun bulkSelectionStopsAtTwoThousandWithoutConsumingLaterItems() {
        val original = (1..2_501).toList()
        val first = MediaTrashBatchPolicy.firstBatch(original)
        assertEquals(2_000, first.size)
        assertEquals(2_000, first.last())
        assertEquals(501, original.size - first.size)
        assertTrue(original.drop(first.size).none { it in first })
    }
}
