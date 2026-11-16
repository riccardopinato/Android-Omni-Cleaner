package com.riccardopinato.omnicleaner.ui

/**
 * Non-destructive preview of selections collected by independent modules.
 * Files and MediaStore URIs may overlap physically, so their bytes are NEVER
 * added together or called "space guaranteed to be reclaimed".
 */
internal data class ReviewQueueEntry(
    val id: String,
    val title: String,
    val sizeBytes: Long,
)

internal data class ReviewCenterSnapshot(
    val filesystemEntries: List<ReviewQueueEntry>,
    val mediaEntries: List<ReviewQueueEntry>,
    val selectedFolders: Int,
    val staleSelections: Int,
) {
    val filesystemIndexedBytes: Long
        get() = filesystemEntries.sumOf { it.sizeBytes.coerceAtLeast(0L) }
    val mediaQueuedBytes: Long
        get() = mediaEntries.sumOf { it.sizeBytes.coerceAtLeast(0L) }
}

internal fun buildReviewCenterSnapshot(
    largeFiles: List<ReviewQueueEntry>,
    duplicateFiles: List<ReviewQueueEntry>,
    androidMedia: List<ReviewQueueEntry>,
    selectedFolders: Int,
    staleSelections: Int,
): ReviewCenterSnapshot = ReviewCenterSnapshot(
    filesystemEntries = (largeFiles + duplicateFiles).distinctBy { it.id },
    mediaEntries = androidMedia.distinctBy { it.id },
    selectedFolders = selectedFolders.coerceAtLeast(0),
    staleSelections = staleSelections.coerceAtLeast(0),
)
