package com.riccardopinato.omnicleaner.gallery

import android.net.Uri
import java.text.DateFormat
import java.util.Date
import java.util.Locale

enum class GalleryMediaFilter(val label: String) {
    ALL("Tutti"),
    IMAGES("Solo Foto"),
    VIDEOS("Solo Video"),
}

enum class GallerySortOrder(val label: String) {
    DATE_DESC("Più recenti"),
    DATE_ASC("Meno recenti"),
    SIZE_DESC("Più grandi"),
    SIZE_ASC("Più piccoli"),
    NAME_ASC("Nome A-Z"),
    ALBUM("Per Album"),
}

enum class SwipeDirection {
    KEEP,
    TRASH,
}

data class GalleryMediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val dateTaken: Long,
    val mimeType: String,
    val album: String?,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val isSelectedForTrash: Boolean = false,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/", ignoreCase = true)

    val formattedDate: String
        get() = if (dateTaken > 0L) {
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(dateTaken))
        } else {
            "Data sconosciuta"
        }

    val formattedDuration: String
        get() {
            if (durationMs <= 0L) return ""
            val totalSeconds = durationMs / 1000L
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }

    val formattedSize: String
        get() = formatMediaSize(sizeBytes)
}

data class GallerySwipeHistoryItem(
    val item: GalleryMediaItem,
    val direction: SwipeDirection,
    val previousIndex: Int,
)

data class GalleryPage(
    val items: List<GalleryMediaItem>,
    val total: Int,
    val hasMore: Boolean,
)

internal fun formatMediaSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}


internal data class GalleryReviewWindowCompaction<T>(
    val items: List<T>,
    val currentIndex: Int,
    val removedPrefix: List<T>,
)

internal fun <T> compactGalleryReviewWindow(
    items: List<T>,
    currentIndex: Int,
    maxReviewedItemsInMemory: Int = 240,
    retainReviewedItems: Int = 60,
): GalleryReviewWindowCompaction<T> {
    val safeCurrentIndex = currentIndex.coerceIn(0, items.size)
    val safeMax = maxReviewedItemsInMemory.coerceAtLeast(1)
    val safeRetain = retainReviewedItems.coerceIn(0, safeMax)

    if (safeCurrentIndex <= safeMax) {
        return GalleryReviewWindowCompaction(
            items = items,
            currentIndex = safeCurrentIndex,
            removedPrefix = emptyList(),
        )
    }

    val removeCount = (safeCurrentIndex - safeRetain)
        .coerceIn(0, safeCurrentIndex)

    return GalleryReviewWindowCompaction(
        items = items.drop(removeCount),
        currentIndex = safeCurrentIndex - removeCount,
        removedPrefix = items.take(removeCount),
    )
}
