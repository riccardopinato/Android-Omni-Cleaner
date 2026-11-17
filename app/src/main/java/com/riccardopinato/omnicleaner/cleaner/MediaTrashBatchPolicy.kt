package com.riccardopinato.omnicleaner.cleaner

/** An Android system Trash confirmation can address at most 2,000 media URIs. */
internal object MediaTrashBatchPolicy {
    const val MAX_URIS_PER_REQUEST = 2_000

    /** Later selections remain pending for a separately confirmed operation. */
    fun <T> firstBatch(items: List<T>): List<T> = items.take(MAX_URIS_PER_REQUEST)
}
