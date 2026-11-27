package com.riccardopinato.omnicleaner.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryTrashEvidenceTest {
    @Test fun onlyConfirmedRowsAreAccepted() {
        val result = evaluateGalleryTrashEvidence(
            requestedIds = setOf(11L, 12L, 13L),
            readAfterWrite = mapOf(11L to true, 12L to false, 13L to null),
        )
        assertEquals(setOf(11L), result.confirmedIds)
        assertEquals(setOf(12L, 13L), result.unverifiedIds)
    }

    @Test fun unknownProviderResultsKeepQueue() {
        val result = evaluateGalleryTrashEvidence(setOf(1L, 2L), emptyMap())
        assertEquals(emptySet<Long>(), result.confirmedIds)
        assertEquals(setOf(1L, 2L), result.unverifiedIds)
    }

    @Test fun unrelatedRowsCannotBeMarkedAsConfirmed() {
        val result = evaluateGalleryTrashEvidence(setOf(1L), mapOf(2L to true))
        assertEquals(emptySet<Long>(), result.confirmedIds)
        assertEquals(setOf(1L), result.unverifiedIds)
    }
}
