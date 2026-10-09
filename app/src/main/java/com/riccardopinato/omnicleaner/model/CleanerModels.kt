package com.riccardopinato.omnicleaner.model

import android.net.Uri
import com.riccardopinato.omnicleaner.cleaner.ThumbnailSnapshot
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val KB = 1024L
private const val MB = 1024L * KB
private const val GB = 1024L * MB

fun formatBytes(bytes: Long): String {
    if (bytes < KB) return "$bytes B"
    val value = when {
        bytes >= GB -> bytes.toDouble() / GB
        bytes >= MB -> bytes.toDouble() / MB
        else -> bytes.toDouble() / KB
    }
    val suffix = when {
        bytes >= GB -> "GB"
        bytes >= MB -> "MB"
        else -> "KB"
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, suffix)
}

enum class FolderStatus {
    COMPLETELY_EMPTY,
    BELOW_THRESHOLD,
}

enum class FolderSubTab {
    EMPTY_FOLDERS,
    APP_RESIDUES,
    THUMBNAIL_CACHE,
}

enum class ThresholdUnit(val label: String, val multiplier: Long) {
    BYTES("B", 1L),
    KB("KB", 1024L),
    MB("MB", 1024L * 1024L),
}

enum class BigFileSizeFilter(val label: String, val minBytes: Long) {
    SIZE_10MB("10 MB", 10L * MB),
    SIZE_50MB("50 MB", 50L * MB),
    SIZE_100MB("100 MB", 100L * MB),
    SIZE_500MB("500 MB", 500L * MB),
    SIZE_1GB("1 GB", GB),
}

enum class GalleryMediaFilter(val label: String) {
    ALL("Tutto"),
    IMAGES("Foto"),
    VIDEOS("Video"),
}

enum class GallerySortOrder(val label: String) {
    DATE_DESC("Più recenti"),
    DATE_ASC("Più vecchi"),
    SIZE_DESC("Più pesanti"),
    SIZE_ASC("Più leggeri"),
    NAME_ASC("Nome"),
    ALBUM("Album"),
}

enum class SwipeDirection {
    KEEP,
    TRASH,
}

enum class ProScanType(val label: String, val description: String) {
    EMPTY_FOLDERS("Cartelle", "Cartelle vuote o molto piccole"),
    LARGE_FILES("File grandi", "File pesanti memorizzati sul dispositivo"),
    DUPLICATES("Duplicati", "Copie identiche rilevate tramite digest"),
    APP_RESIDUES("Residui", "Cartelle di app non più installate"),
    THUMBNAIL_CACHE("Miniature", "Cache di thumbnail ricreabili"),
}

data class FolderItem(
    val file: File,
    val name: String = file.name.ifBlank { file.path },
    val path: String = file.absolutePath,
    val displayPath: String = file.absolutePath,
    val sizeBytes: Long,
    val status: FolderStatus,
    val isSelected: Boolean = false,
    val fileCount: Int = 0,
    val subfolderCount: Int = 0,
) {
    val formattedSize: String get() = formatBytes(sizeBytes)
}

data class BigFileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val displayPath: String = file.absolutePath,
    val sizeBytes: Long = file.length(),
    val lastModified: Long = file.lastModified(),
    val extension: String = file.extension.lowercase(),
    val isImageOrVideo: Boolean = extension in IMAGE_EXTENSIONS || extension in VIDEO_EXTENSIONS,
    val isSelected: Boolean = false,
) {
    val fileTypeDescription: String
        get() = when {
            extension in IMAGE_EXTENSIONS -> "Immagine"
            extension in VIDEO_EXTENSIONS -> "Video"
            extension in AUDIO_EXTENSIONS -> "Audio"
            extension in ARCHIVE_EXTENSIONS -> "Archivio"
            extension == "apk" -> "APK"
            else -> extension.uppercase().ifBlank { "File" }
        }
}

data class DuplicateFile(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val displayPath: String = file.absolutePath,
    val sizeBytes: Long = file.length(),
    val lastModified: Long = file.lastModified(),
    val extension: String = file.extension.lowercase(),
    val isSelected: Boolean = false,
) {
    val formattedSize: String get() = formatBytes(sizeBytes)
}

data class DuplicateGroup(
    val hash: String,
    val sizeBytes: Long,
    val files: List<DuplicateFile>,
) {
    val totalDuplicatesCount: Int get() = (files.size - 1).coerceAtLeast(0)
    val selectedFilesCount: Int get() = files.count { it.isSelected }
    val totalWasteBytes: Long get() = sizeBytes * totalDuplicatesCount
}

data class AppResidueItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val displayPath: String = file.absolutePath,
    val sizeBytes: Long,
    val fileCount: Int,
    val isSelected: Boolean = false,
) {
    val formattedSize: String get() = formatBytes(sizeBytes)
}

data class ThumbnailCacheItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val displayPath: String = file.absolutePath,
    val sizeBytes: Long,
    val fileCount: Int,
    val isSelected: Boolean = false,
    val warning: String? = null,
    val inventory: ThumbnailSnapshot? = null,
) {
    val formattedSize: String get() = formatBytes(sizeBytes)
}

data class GalleryMediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val dateTaken: Long,
    val mimeType: String,
    val album: String,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val isSelectedForTrash: Boolean = false,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val formattedSize: String get() = formatBytes(sizeBytes)
    val formattedDate: String
        get() = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(dateTaken))
    val formattedDuration: String
        get() {
            val seconds = durationMs / 1000
            return "%d:%02d".format(seconds / 60, seconds % 60)
        }
}

data class GallerySwipeHistoryItem(
    val item: GalleryMediaItem,
    val direction: SwipeDirection,
    val previousIndex: Int,
)

data class ProScanSelection(
    val selectedTypes: Set<ProScanType> = ProScanType.entries.toSet(),
    val isScanning: Boolean = false,
    val completedTypes: Set<ProScanType> = emptySet(),
    val totalRecoverableBytes: Long = 0L,
    val showReviewSheet: Boolean = false,
    val showResolveConfirmDialog: Boolean = false,
    val hasCompletedScan: Boolean = false,
)

val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp")
val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "m4v")
val AUDIO_EXTENSIONS = setOf("mp3", "wav", "flac", "m4a", "aac", "ogg", "opus")
val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
