package com.riccardopinato.omnicleaner.cleaner

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import java.util.UUID

class SafeTrashManager private constructor(
    private val trashDirectory: File,
) {
    constructor(context: Context) : this(File(context.filesDir, "trash"))

    init {
        trashDirectory.mkdirs()
    }

    fun getTrashDirectory(): File = trashDirectory

    fun getTrashSize(): Long =
        trashDirectory
            .walkBottomUp()
            .filter { it.isFile }
            .sumOf { it.length().coerceAtLeast(0L) }

    fun moveToTrash(file: File): File? {
        if (!file.exists() || !file.isFile) return null
        if (PathSafety.isDangerousPath(file) || PathSafety.isSymlink(file)) return null

        // MediaStore-owned user media is intentionally routed through the
        // platform trash flow instead of this private filesystem trash.
        if (MediaStoreHelper.isMediaFile(file)) return null

        val originalSize = file.length().coerceAtLeast(0L)
        val originalModified = file.lastModified().coerceAtLeast(0L)

        val entry = File(trashDirectory, UUID.randomUUID().toString())
        val payload = File(entry, PAYLOAD_NAME)
        val metadata = File(entry, METADATA_NAME)

        if (!entry.mkdirs()) return null

        val metadataWritten = runCatching {
            val properties = Properties().apply {
                setProperty(KEY_ORIGINAL_PATH, file.absolutePath)
                setProperty(KEY_ORIGINAL_NAME, file.name)
                setProperty(KEY_SIZE_BYTES, originalSize.toString())
                setProperty(KEY_LAST_MODIFIED, originalModified.toString())
            }
            FileOutputStream(metadata).use { output ->
                properties.store(output, METADATA_COMMENT)
                output.fd.sync()
            }
            true
        }.getOrDefault(false)

        if (!metadataWritten) {
            entry.deleteRecursively()
            return null
        }

        if (file.renameTo(payload)) {
            return payload
        }

        if (originalSize > MAX_SMALL_FILE_TRASH_BYTES) {
            entry.deleteRecursively()
            return null
        }

        val requiredFreeSpace = originalSize + COPY_FREE_SPACE_MARGIN_BYTES
        if (trashDirectory.usableSpace in 0 until requiredFreeSpace) {
            entry.deleteRecursively()
            return null
        }

        val copied = runCatching {
            FileInputStream(file).use { input ->
                FileOutputStream(payload).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }

            if (file.length() != originalSize || file.lastModified() != originalModified) {
                error("Source changed")
            }
            if (payload.length() != originalSize) {
                error("Incomplete copy")
            }
            if (!sameContents(file, payload, originalSize)) {
                error("Incomplete copy")
            }
            true
        }.getOrDefault(false)

        if (!copied || !file.delete()) {
            payload.delete()
            entry.deleteRecursively()
            return null
        }

        return payload
    }

    fun permanentlyDelete(file: File): Boolean {
        val target = runCatching { file.canonicalFile }.getOrNull() ?: return false
        val root = runCatching { trashDirectory.canonicalFile }.getOrNull() ?: return false
        val rootPrefix = root.path.trimEnd(File.separatorChar) + File.separator
        val targetPath = target.path

        if (target == root || !targetPath.startsWith(rootPrefix)) return false
        return if (target.isDirectory) target.deleteRecursively() else target.delete()
    }

    fun emptyTrash(): Boolean {
        val entries = trashDirectory.listFiles() ?: return true
        return entries.all { it.deleteRecursively() }
    }

    fun sameContents(first: File, second: File, expectedBytes: Long): Boolean {
        if (!first.isFile || !second.isFile) return false
        if (first.length() != expectedBytes || second.length() != expectedBytes) return false

        FileInputStream(first).use { left ->
            FileInputStream(second).use { right ->
                val leftBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
                val rightBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var compared = 0L

                while (true) {
                    val leftRead = left.read(leftBuffer)
                    val rightRead = right.read(rightBuffer)
                    if (leftRead != rightRead) return false
                    if (leftRead <= 0) break

                    compared += leftRead
                    for (index in 0 until leftRead) {
                        if (leftBuffer[index] != rightBuffer[index]) return false
                    }
                }

                return compared == expectedBytes
            }
        }
    }

    internal companion object {
        const val MAX_SMALL_FILE_TRASH_BYTES: Long = 10L * 1024L * 1024L
        private const val COPY_FREE_SPACE_MARGIN_BYTES: Long = 1L * 1024L * 1024L
        private const val PAYLOAD_NAME = "payload"
        private const val METADATA_NAME = "original.properties"
        private const val METADATA_COMMENT = "Omni Cleaner trash entry"

        private const val KEY_ORIGINAL_PATH = "originalPath"
        private const val KEY_ORIGINAL_NAME = "originalName"
        private const val KEY_SIZE_BYTES = "sizeBytes"
        private const val KEY_LAST_MODIFIED = "lastModified"

        internal fun forTests(trashDirectory: File): SafeTrashManager =
            SafeTrashManager(trashDirectory)
    }
}
