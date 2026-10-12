package com.riccardopinato.omnicleaner.cleaner

import java.io.File
import java.util.Locale

enum class SmartCategory {
    OLD_DOWNLOADS,
    INSTALLERS,
    ARCHIVES,
    SCREENSHOTS,
}

data class SmartCategoryItem(
    val file: File,
    val name: String,
    val path: String,
    val displayPath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String,
    val categories: Set<SmartCategory>,
)

data class SmartCategoryScan(
    val items: List<SmartCategoryItem>,
    val unreadable: Int,
    val excludedDirectories: Int,
) {
    val totalUniqueBytes: Long
        get() = items.sumOf { it.sizeBytes }

    fun itemsFor(category: SmartCategory): List<SmartCategoryItem> =
        items.filter { category in it.categories }

    fun bytesFor(category: SmartCategory): Long =
        items.asSequence()
            .filter { category in it.categories }
            .sumOf { it.sizeBytes }
}

class SmartCategoryScanner {
    suspend fun scan(
        root: File,
        nowMillis: Long = System.currentTimeMillis(),
        oldDownloadAgeMillis: Long = DEFAULT_OLD_DOWNLOAD_AGE_MILLIS,
    ): SmartCategoryScan {
        val inventory = StorageInventoryRepository.snapshot(root).inventory
        return classify(
            root = root,
            files = inventory.files,
            nowMillis = nowMillis,
            oldDownloadAgeMillis = oldDownloadAgeMillis,
            unreadable = inventory.unreadable,
            excludedDirectories = inventory.excludedDirectories,
        )
    }

    internal fun classify(
        root: File,
        files: List<IndexedFile>,
        nowMillis: Long,
        oldDownloadAgeMillis: Long = DEFAULT_OLD_DOWNLOAD_AGE_MILLIS,
        unreadable: Int = 0,
        excludedDirectories: Int = 0,
    ): SmartCategoryScan {
        val normalizedRoot = canonicalPath(root).trimEnd('/')
        val rootPrefix = if (normalizedRoot.isBlank()) "/" else "$normalizedRoot/"
        val age = oldDownloadAgeMillis.coerceAtLeast(0L)
        val oldThreshold = (nowMillis.coerceAtLeast(0L) - age).coerceAtLeast(0L)

        val items = files.mapNotNull { indexed ->
            val filePath = canonicalPath(indexed.file)
            if (!filePath.startsWith(rootPrefix)) return@mapNotNull null

            val relative = filePath.removePrefix(rootPrefix)
            val segments = relative.split('/').filter { it.isNotBlank() }
            if (segments.isEmpty()) return@mapNotNull null

            val topLevel = segments.first().lowercase(Locale.ROOT)
            val lowerSegments = segments.map { it.lowercase(Locale.ROOT) }
            val lowerName = indexed.file.name.lowercase(Locale.ROOT)
            val extension = indexed.file.extension.lowercase(Locale.ROOT)
            val categories = linkedSetOf<SmartCategory>()

            val isDownload = topLevel in DOWNLOAD_DIRECTORIES
            if (
                isDownload &&
                indexed.modified > 0L &&
                indexed.modified <= oldThreshold
            ) {
                categories += SmartCategory.OLD_DOWNLOADS
            }

            if (extension in INSTALLER_EXTENSIONS) {
                categories += SmartCategory.INSTALLERS
            }

            if (extension in ARCHIVE_EXTENSIONS) {
                categories += SmartCategory.ARCHIVES
            }

            if (
                lowerSegments.any { it in SCREENSHOT_DIRECTORIES } ||
                SCREENSHOT_NAME_PREFIXES.any { lowerName.startsWith(it) }
            ) {
                categories += SmartCategory.SCREENSHOTS
            }

            if (categories.isEmpty()) return@mapNotNull null

            SmartCategoryItem(
                file = indexed.file,
                name = indexed.file.name,
                path = filePath,
                displayPath = filePath,
                sizeBytes = indexed.size.coerceAtLeast(0L),
                lastModified = indexed.modified.coerceAtLeast(0L),
                extension = extension,
                categories = categories,
            )
        }.sortedWith(
            compareByDescending<SmartCategoryItem> { it.sizeBytes }
                .thenBy { it.path },
        )

        return SmartCategoryScan(
            items = items,
            unreadable = unreadable.coerceAtLeast(0),
            excludedDirectories = excludedDirectories.coerceAtLeast(0),
        )
    }

    private fun canonicalPath(file: File): String =
        runCatching { file.canonicalPath }
            .getOrElse { file.absolutePath }
            .replace('\\', '/')

    companion object {
        const val DEFAULT_OLD_DOWNLOAD_DAYS: Long = 90L
        const val DEFAULT_OLD_DOWNLOAD_AGE_MILLIS: Long =
            DEFAULT_OLD_DOWNLOAD_DAYS * 24L * 60L * 60L * 1000L

        private val DOWNLOAD_DIRECTORIES = setOf("download", "downloads")
        private val INSTALLER_EXTENSIONS = setOf("apk", "apks", "xapk", "apkm")
        private val ARCHIVE_EXTENSIONS = setOf(
            "zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz",
        )
        private val SCREENSHOT_DIRECTORIES = setOf("screenshot", "screenshots")
        private val SCREENSHOT_NAME_PREFIXES = listOf(
            "screenshot", "screen_shot", "screen-shot",
        )
    }
}
