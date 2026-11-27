package com.riccardopinato.omnicleaner.gallery

/** Android's approval callback does not prove a provider mutation. */
internal data class GalleryTrashEvidence(
    val confirmedIds: Set<Long>,
    val unverifiedIds: Set<Long>,
)

internal fun evaluateGalleryTrashEvidence(
    requestedIds: Set<Long>,
    readAfterWrite: Map<Long, Boolean?>,
): GalleryTrashEvidence {
    val confirmed = requestedIds.filterTo(linkedSetOf()) { readAfterWrite[it] == true }
    return GalleryTrashEvidence(confirmed, requestedIds - confirmed)
}
