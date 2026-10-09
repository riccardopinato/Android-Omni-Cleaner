package com.riccardopinato.omnicleaner.cleaner

import android.content.Context
import java.io.File
import java.security.MessageDigest

class SafeTrashManager(context: Context) {
    companion object {
        const val MAX_SMALL_FILE_TRASH_BYTES: Long = 100L * 1024L * 1024L
    }

    private val trashDirectory = File(context.filesDir, ".omnicleaner-trash").apply { mkdirs() }

    fun getTrashDirectory(): File = trashDirectory

    fun getTrashSize(): Long = directorySize(trashDirectory)

    fun emptyTrash(): Boolean {
        val children = trashDirectory.listFiles().orEmpty()
        return children.all { it.deleteRecursively() }
    }

    fun moveToTrash(source: File): File? {
        if (!source.exists()) return null
        if (source.isDirectory && !PathSafety.isSafeFolderToDelete(source)) return null

        val size = if (source.isDirectory) directorySize(source) else source.length()
        if (size > MAX_SMALL_FILE_TRASH_BYTES) return null

        trashDirectory.mkdirs()
        val destination = uniqueDestination(source.name)
        val copied = runCatching {
            if (source.isDirectory) {
                source.copyRecursively(destination, overwrite = false)
            } else {
                source.copyTo(destination, overwrite = false)
                true
            }
        }.getOrDefault(false)

        if (!copied || !sameContents(source, destination, size)) {
            destination.deleteRecursively()
            return null
        }

        val deleted = if (source.isDirectory) source.deleteRecursively() else source.delete()
        if (!deleted) {
            destination.deleteRecursively()
            return null
        }
        return destination
    }

    fun permanentlyDelete(source: File): Boolean {
        if (!source.exists()) return true
        if (source.isDirectory && !PathSafety.isSafeFolderToDelete(source)) return false
        return if (source.isDirectory) source.deleteRecursively() else source.delete()
    }

    private fun uniqueDestination(name: String): File {
        val safeName = name.ifBlank { "item" }
        var candidate = File(trashDirectory, safeName)
        var index = 1
        while (candidate.exists()) {
            candidate = File(trashDirectory, "$safeName.$index")
            index++
        }
        return candidate
    }

    private fun sameContents(source: File, copy: File, expectedBytes: Long): Boolean {
        if (!copy.exists()) return false
        val copySize = if (copy.isDirectory) directorySize(copy) else copy.length()
        if (copySize != expectedBytes) return false
        if (source.isDirectory) return true
        if (expectedBytes > 8L * 1024L * 1024L) return true
        return digest(source) == digest(copy)
    }

    private fun digest(file: File): String = runCatching {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                md.update(buffer, 0, read)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }.getOrDefault("")

    private fun directorySize(root: File): Long {
        if (!root.exists()) return 0L
        var total = 0L
        val queue = ArrayDeque<File>()
        queue += root
        while (queue.isNotEmpty()) {
            val current = queue.removeLast()
            if (current.isDirectory) current.listFiles()?.forEach(queue::add)
            else if (current.isFile) total += current.length()
        }
        return total
    }
}
