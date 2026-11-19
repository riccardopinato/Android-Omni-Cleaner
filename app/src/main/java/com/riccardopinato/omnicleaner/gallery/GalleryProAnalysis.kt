package com.riccardopinato.omnicleaner.gallery

import java.util.Locale

/** Conservative, read-only recommendations; results are NEVER Trash decisions. */
internal data class GalleryVideoCandidate(
    val id: Long,
    val name: String,
    val bytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val album: String?,
)

internal enum class GalleryVideoReviewFilter {
    ALL, LARGE_100_MB, LONG_5_MIN, UHD_4K,
}

/**
 * Inclusive, metadata-only size bounds for advisory Gallery Pro filtering.
 * Never represents recoverable space or a deletion selection.
 */
internal data class GalleryVideoSizeRange(
    val minimumBytes: Long,
    val maximumBytes: Long,
) {
    init {
        require(minimumBytes >= 0L) { "Minimum video size must be non-negative" }
        require(maximumBytes >= minimumBytes) { "Maximum must not precede minimum" }
    }

    fun contains(bytes: Long): Boolean = bytes in minimumBytes..maximumBytes
}

internal data class GalleryVideoReviewResult(
    val totalMatching: Int,
    val results: List<GalleryVideoCandidate>,
    val bytesMatching: Long,
)

internal fun filterGalleryVideos(
    candidates: List<GalleryVideoCandidate>,
    filter: GalleryVideoReviewFilter,
    text: String = "",
    maxVisible: Int = 100,
    sizeRange: GalleryVideoSizeRange? = null,
): GalleryVideoReviewResult {
    val query = text.trim().lowercase(Locale.ROOT)
    val max = maxVisible.coerceIn(0, 200)
    val matching = candidates.asSequence()
        .filter { video ->
            when (filter) {
                GalleryVideoReviewFilter.ALL -> true
                GalleryVideoReviewFilter.LARGE_100_MB -> video.bytes >= 100L * 1024L * 1024L
                GalleryVideoReviewFilter.LONG_5_MIN -> video.durationMs >= 5L * 60L * 1000L
                GalleryVideoReviewFilter.UHD_4K -> maxOf(video.width, video.height) >= 3840
            }
        }
        .filter { sizeRange == null || sizeRange.contains(it.bytes) }
        .filter { it.name.lowercase(Locale.ROOT).contains(query) ||
            it.album.orEmpty().lowercase(Locale.ROOT).contains(query) }
        .sortedWith(compareByDescending<GalleryVideoCandidate> { it.bytes }.thenBy { it.id })
        .toList()
    return GalleryVideoReviewResult(
        totalMatching = matching.size,
        results = matching.take(max),
        bytesMatching = matching.sumOf { it.bytes.coerceAtLeast(0) },
    )
}

/** A thumbnail fingerprint, NOT proof that two files are identical. */
internal data class GalleryPhotoSignature(
    val id: Long,
    val timestampMs: Long,
    val differenceHash: Long,
)

internal data class GallerySimilarPhotoGroup(
    val photoIds: List<Long>,
)

/**
 * Bounded clustering of nearby captures based on a perceptual 64-bit dHash.
 * Temporal + visual tests must BOTH pass. Confirms no deletion, chooses no keeper.
 */
internal fun groupSimilarGalleryPhotos(
    fingerprints: List<GalleryPhotoSignature>,
    maxTemporalDistanceMs: Long = 60_000L,
    maxHammingDistance: Int = 8,
    maxPhotos: Int = 160,
): List<GallerySimilarPhotoGroup> {
    val safe = fingerprints.asSequence()
        .filter { it.id > 0L && it.timestampMs > 0L }
        .distinctBy { it.id }
        .take(maxPhotos.coerceIn(0, 160))
        .sortedBy { it.timestampMs }
        .toList()
    if (safe.size < 2) return emptyList()
    val parents = IntArray(safe.size) { it }
    fun find(x: Int): Int {
        var p = x
        while (parents[p] != p) {
            parents[p] = parents[parents[p]]
            p = parents[p]
        }
        return p
    }
    fun union(a: Int, b: Int) {
        val rootA = find(a)
        val rootB = find(b)
        if (rootA != rootB) parents[rootB] = rootA
    }
    val timeWindow = maxTemporalDistanceMs.coerceIn(0L, 10L * 60_000L)
    val hammingLimit = maxHammingDistance.coerceIn(0, 16)
    for (i in safe.indices) {
        var j = i + 1
        while (j < safe.size &&
            safe[j].timestampMs - safe[i].timestampMs <= timeWindow
        ) {
            val bitDifference = java.lang.Long.bitCount(
                safe[i].differenceHash xor safe[j].differenceHash
            )
            if (bitDifference <= hammingLimit) union(i, j)
            j++
        }
    }
    return safe.indices.groupBy { find(it) }.values
        .filter { it.size >= 2 }
        .map { indices -> GallerySimilarPhotoGroup(indices.map { safe[it].id }) }
        .sortedByDescending { it.photoIds.size }
}
