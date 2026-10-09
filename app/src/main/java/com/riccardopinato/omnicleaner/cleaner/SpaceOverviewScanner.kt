package com.riccardopinato.omnicleaner.cleaner

import com.riccardopinato.omnicleaner.model.ARCHIVE_EXTENSIONS
import com.riccardopinato.omnicleaner.model.AUDIO_EXTENSIONS
import com.riccardopinato.omnicleaner.model.IMAGE_EXTENSIONS
import com.riccardopinato.omnicleaner.model.VIDEO_EXTENSIONS
import java.io.File
import java.util.PriorityQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class SpaceEntry(
    val path: String,
    val bytes: Long,
)

data class SpaceOverview(
    val logicalBytes: Long,
    val files: Long,
    val unreadable: Long,
    val excludedDirectories: Long,
    val byCategory: Map<String, Long>,
    val largest: List<SpaceEntry>,
)

object SpaceOverviewScanner {
    suspend fun scan(root: File, largestLimit: Int = 12): SpaceOverview = withContext(Dispatchers.IO) {
        var bytes = 0L
        var files = 0L
        var unreadable = 0L
        var excluded = 0L
        val categories = linkedMapOf(
            "Immagini" to 0L,
            "Video" to 0L,
            "Audio" to 0L,
            "Documenti" to 0L,
            "Archivi" to 0L,
            "APK" to 0L,
            "Altro" to 0L,
        )
        val largest = PriorityQueue<SpaceEntry>(compareBy { it.bytes })
        val queue = ArrayDeque<File>()
        val seen = HashSet<String>()
        queue += root

        while (queue.isNotEmpty()) {
            coroutineContext.ensureActive()
            val current = queue.removeLast()
            if (FileInventoryPolicy.shouldIgnore(current, root)) {
                if (current.isDirectory) excluded++
                continue
            }

            if (current.isDirectory) {
                val canonical = runCatching { current.canonicalPath }.getOrNull()
                if (canonical == null || !seen.add(canonical)) {
                    unreadable++
                    continue
                }
                val children = runCatching { current.listFiles() }.getOrNull()
                if (children == null) unreadable++ else children.forEach(queue::add)
                continue
            }

            if (!current.isFile) continue
            val size = runCatching { current.length() }.getOrDefault(0L)
            files++
            bytes += size
            val category = categoryFor(current)
            categories[category] = categories.getValue(category) + size

            if (largestLimit > 0) {
                largest += SpaceEntry(current.absolutePath, size)
                if (largest.size > largestLimit) largest.poll()
            }
        }

        SpaceOverview(
            logicalBytes = bytes,
            files = files,
            unreadable = unreadable,
            excludedDirectories = excluded,
            byCategory = categories,
            largest = largest.toList().sortedByDescending { it.bytes },
        )
    }

    private fun categoryFor(file: File): String {
        val ext = file.extension.lowercase()
        return when {
            ext in IMAGE_EXTENSIONS -> "Immagini"
            ext in VIDEO_EXTENSIONS -> "Video"
            ext in AUDIO_EXTENSIONS -> "Audio"
            ext in ARCHIVE_EXTENSIONS -> "Archivi"
            ext == "apk" -> "APK"
            ext in setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "odt") -> "Documenti"
            else -> "Altro"
        }
    }
}
