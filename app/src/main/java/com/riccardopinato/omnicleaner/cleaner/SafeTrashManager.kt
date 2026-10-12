package com.riccardopinato.omnicleaner.cleaner

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID

data class SafeTrashEntry(
    val id: String,
    val originalPath: String,
    val originalName: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val sha256: String,
)

enum class SafeTrashRestoreStatus {
    RESTORED,
    ENTRY_NOT_FOUND,
    PAYLOAD_INVALID,
    DESTINATION_EXISTS,
    DESTINATION_UNAVAILABLE,
    UNSAFE_DESTINATION,
    INSUFFICIENT_SPACE,
    COPY_FAILED,
}

data class SafeTrashRestoreResult(
    val status: SafeTrashRestoreStatus,
    val restoredFile: File? = null,
)

class SafeTrashManager private constructor(
    private val trashDirectory: File,
    private val restoreRoot: File,
    private val legacyTrashDirectory: File? = null,
) {
    constructor(context: Context) : this(
        trashDirectory = StorageAccess.safeTrashRoot(),
        restoreRoot = StorageAccess.primaryRoot(),
        legacyTrashDirectory = File(context.filesDir, "trash"),
    )

    init {
        preparePersistentTrash()
        migrateLegacyTrash()
    }

    fun getTrashDirectory(): File = trashDirectory

    fun isPersistentTrashAvailable(): Boolean =
        trashDirectory.isDirectory &&
            !PathSafety.isSymlink(trashDirectory) &&
            PathSafety.isWithinRoot(trashDirectory, restoreRoot)

    fun listEntries(): List<SafeTrashEntry> {
        if (!isPersistentTrashAvailable()) return emptyList()
        return trashDirectory.listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isDirectory }
            .mapNotNull(::readEntry)
            .sortedByDescending { entry ->
                File(trashDirectory, entry.id).lastModified()
            }
            .toList()
    }

    fun getTrashSize(): Long {
        if (!isPersistentTrashAvailable()) return 0L
        return trashDirectory.listFiles()
            .orEmpty()
            .sumOf { entry ->
                File(entry, PAYLOAD_NAME)
                    .takeIf { it.isFile && !PathSafety.isSymlink(it) }
                    ?.length()
                    ?.coerceAtLeast(0L)
                    ?: 0L
            }
    }

    fun moveToTrash(snapshot: CleanupFileSnapshot): File? {
        if (!CleanupSnapshotValidator.validate(snapshot).valid) return null
        return moveToTrashInternal(
            file = snapshot.file,
            expectedSize = snapshot.expectedBytes,
            expectedModified = snapshot.expectedModified,
            expectedHash = snapshot.expectedSha256,
        )
    }

    fun moveToTrash(file: File): File? =
        moveToTrashInternal(
            file = file,
            expectedSize = null,
            expectedModified = null,
            expectedHash = null,
        )

    private fun moveToTrashInternal(
        file: File,
        expectedSize: Long?,
        expectedModified: Long?,
        expectedHash: String?,
    ): File? {
        if (!isPersistentTrashAvailable()) return null
        if (!file.exists() || !file.isFile) return null
        if (!PathSafety.isWithinRoot(file, restoreRoot)) return null
        if (PathSafety.isDangerousPath(file) || PathSafety.isSymlink(file)) return null
        if (MediaStoreHelper.isMediaFile(file)) return null

        val originalSize = file.length().coerceAtLeast(0L)
        val originalModified = file.lastModified().coerceAtLeast(0L)
        if (originalSize > MAX_SMALL_FILE_TRASH_BYTES) return null
        if (expectedSize != null && originalSize != expectedSize) return null
        if (expectedModified != null && originalModified != expectedModified) return null

        val originalHash = runCatching { sha256(file) }.getOrNull() ?: return null
        if (expectedHash != null && originalHash != expectedHash) return null
        if (
            file.length() != originalSize ||
            file.lastModified() != originalModified
        ) {
            return null
        }

        val entry = File(trashDirectory, UUID.randomUUID().toString())
        val payload = File(entry, PAYLOAD_NAME)
        val metadata = File(entry, METADATA_NAME)

        if (!entry.mkdirs()) return null

        val metadataWritten = runCatching {
            val properties = Properties().apply {
                setProperty(KEY_ORIGINAL_PATH, file.canonicalPath)
                setProperty(KEY_ORIGINAL_NAME, file.name)
                setProperty(KEY_SIZE_BYTES, originalSize.toString())
                setProperty(KEY_LAST_MODIFIED, originalModified.toString())
                setProperty(KEY_SHA256, originalHash)
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
            val validPayload =
                payload.isFile &&
                    payload.length() == originalSize &&
                    runCatching { sha256(payload) }.getOrNull() == originalHash
            if (validPayload) return payload

            runCatching {
                if (!file.exists()) payload.renameTo(file)
            }
            entry.deleteRecursively()
            return null
        }

        val requiredFreeSpace = originalSize + COPY_FREE_SPACE_MARGIN_BYTES
        if (trashDirectory.usableSpace in 0L until requiredFreeSpace) {
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
            if (sha256(payload) != originalHash) {
                error("Payload digest mismatch")
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

    fun restore(entryId: String): SafeTrashRestoreResult {
        if (!isPersistentTrashAvailable()) {
            return SafeTrashRestoreResult(SafeTrashRestoreStatus.DESTINATION_UNAVAILABLE)
        }
        if (!ENTRY_ID_PATTERN.matches(entryId)) {
            return SafeTrashRestoreResult(SafeTrashRestoreStatus.ENTRY_NOT_FOUND)
        }

        val directory = File(trashDirectory, entryId)
        val entry = readEntry(directory)
            ?: return SafeTrashRestoreResult(SafeTrashRestoreStatus.ENTRY_NOT_FOUND)
        val payload = File(directory, PAYLOAD_NAME)

        if (
            !payload.isFile ||
            PathSafety.isSymlink(payload) ||
            payload.length() != entry.sizeBytes ||
            runCatching { sha256(payload) }.getOrNull() != entry.sha256
        ) {
            return SafeTrashRestoreResult(SafeTrashRestoreStatus.PAYLOAD_INVALID)
        }

        val destination = runCatching { File(entry.originalPath).canonicalFile }.getOrNull()
            ?: return SafeTrashRestoreResult(SafeTrashRestoreStatus.DESTINATION_UNAVAILABLE)

        if (
            !PathSafety.isWithinRoot(destination, restoreRoot) ||
            PathSafety.isDangerousPath(destination)
        ) {
            return SafeTrashRestoreResult(SafeTrashRestoreStatus.UNSAFE_DESTINATION)
        }

        if (destination.exists()) {
            return SafeTrashRestoreResult(SafeTrashRestoreStatus.DESTINATION_EXISTS)
        }

        val parent = destination.parentFile
            ?: return SafeTrashRestoreResult(SafeTrashRestoreStatus.DESTINATION_UNAVAILABLE)
        if (
            !parent.isDirectory ||
            !parent.canWrite() ||
            PathSafety.isSymlink(parent) ||
            !PathSafety.isWithinOrEqualRoot(parent, restoreRoot)
        ) {
            return SafeTrashRestoreResult(SafeTrashRestoreStatus.DESTINATION_UNAVAILABLE)
        }

        val requiredFreeSpace = entry.sizeBytes + COPY_FREE_SPACE_MARGIN_BYTES
        if (parent.usableSpace in 0L until requiredFreeSpace) {
            return SafeTrashRestoreResult(SafeTrashRestoreStatus.INSUFFICIENT_SPACE)
        }

        val temp = File(
            parent,
            ".${destination.name}.omni-restore-${UUID.randomUUID()}.tmp",
        )
        val copied = runCatching {
            FileInputStream(payload).use { input ->
                FileOutputStream(temp).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            temp.length() == entry.sizeBytes && sha256(temp) == entry.sha256
        }.getOrDefault(false)

        if (!copied) {
            temp.delete()
            return SafeTrashRestoreResult(SafeTrashRestoreStatus.COPY_FAILED)
        }

        if (destination.exists() || !temp.renameTo(destination)) {
            temp.delete()
            return SafeTrashRestoreResult(
                if (destination.exists()) {
                    SafeTrashRestoreStatus.DESTINATION_EXISTS
                } else {
                    SafeTrashRestoreStatus.COPY_FAILED
                },
            )
        }

        if (
            destination.length() != entry.sizeBytes ||
            runCatching { sha256(destination) }.getOrNull() != entry.sha256
        ) {
            destination.delete()
            return SafeTrashRestoreResult(SafeTrashRestoreStatus.COPY_FAILED)
        }

        if (entry.lastModified > 0L) {
            destination.setLastModified(entry.lastModified)
        }

        directory.deleteRecursively()

        return SafeTrashRestoreResult(
            status = SafeTrashRestoreStatus.RESTORED,
            restoredFile = destination,
        )
    }

    fun permanentlyDelete(file: File): Boolean {
        if (!isPersistentTrashAvailable()) return false
        val root = runCatching { trashDirectory.canonicalFile }.getOrNull() ?: return false
        val rootPrefix = root.path.trimEnd(File.separatorChar) + File.separator
        val absolute = file.absoluteFile

        if (!absolute.path.startsWith(rootPrefix)) return false
        if (PathSafety.isSymlink(file)) {
            // File.delete() removes the link itself and does not recurse into
            // the link target.
            return file.delete()
        }

        val target = runCatching { file.canonicalFile }.getOrNull() ?: return false
        if (target == root || !target.path.startsWith(rootPrefix)) return false
        return deleteTreeWithoutFollowingSymlinks(target, root)
    }

    fun emptyTrash(): Boolean {
        if (!isPersistentTrashAvailable()) return false
        val entries = trashDirectory.listFiles() ?: return true
        var allDeleted = true
        for (entry in entries) {
            if (!permanentlyDelete(entry)) allDeleted = false
        }
        return allDeleted
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

    private fun deleteTreeWithoutFollowingSymlinks(node: File, root: File): Boolean {
        val rootPrefix = root.path.trimEnd(File.separatorChar) + File.separator
        val absolute = node.absoluteFile
        if (!absolute.path.startsWith(rootPrefix)) return false

        if (PathSafety.isSymlink(node)) {
            return node.delete()
        }

        val canonical = runCatching { node.canonicalFile }.getOrNull() ?: return false
        if (canonical != node.absoluteFile || !canonical.path.startsWith(rootPrefix)) return false

        if (canonical.isDirectory) {
            var childrenDeleted = true
            for (child in canonical.listFiles().orEmpty()) {
                if (!deleteTreeWithoutFollowingSymlinks(child, root)) childrenDeleted = false
            }
            if (!childrenDeleted) return false
        }

        return canonical.delete()
    }

    private fun preparePersistentTrash() {
        val parent = trashDirectory.parentFile ?: return
        if (PathSafety.isSymlink(parent) || PathSafety.isSymlink(trashDirectory)) return
        if (!parent.exists() && !parent.mkdirs()) return
        if (!trashDirectory.exists() && !trashDirectory.mkdirs()) return
        runCatching { File(parent, ".nomedia").createNewFile() }
    }

    /**
     * Recovery builds before 0.11 stored reversible payloads under filesDir.
     * Copy each verified legacy entry into persistent shared storage first and
     * delete the legacy copy only after the migrated entry can be parsed and
     * integrity-checked. A failed migration leaves the original untouched.
     */
    private fun migrateLegacyTrash() {
        val legacyRoot = legacyTrashDirectory ?: return
        if (!legacyRoot.isDirectory || !isPersistentTrashAvailable()) return

        for (legacyEntry in legacyRoot.listFiles().orEmpty()) {
            if (!legacyEntry.isDirectory || !ENTRY_ID_PATTERN.matches(legacyEntry.name)) continue

            val target = File(trashDirectory, legacyEntry.name)
            if (target.exists()) continue

            val legacyMetadata = File(legacyEntry, METADATA_NAME)
            val legacyPayload = File(legacyEntry, PAYLOAD_NAME)
            if (!legacyMetadata.isFile || !legacyPayload.isFile || PathSafety.isSymlink(legacyPayload)) continue
            if (!target.mkdirs()) continue

            val targetMetadata = File(target, METADATA_NAME)
            val targetPayload = File(target, PAYLOAD_NAME)
            val sourcePayloadHash = runCatching { sha256(legacyPayload) }.getOrNull()

            val copied = runCatching {
                copyAndSync(legacyMetadata, targetMetadata)
                copyAndSync(legacyPayload, targetPayload)
                true
            }.getOrDefault(false)

            val migratedEntry = if (copied) readEntry(target) else null
            val copyVerified =
                sourcePayloadHash != null &&
                    migratedEntry != null &&
                    targetPayload.isFile &&
                    targetPayload.length() == legacyPayload.length() &&
                    runCatching { sha256(targetPayload) }.getOrNull() == sourcePayloadHash &&
                    runCatching { sha256(targetPayload) }.getOrNull() == migratedEntry.sha256

            if (!copyVerified) {
                target.deleteRecursively()
                continue
            }

            legacyEntry.deleteRecursively()
        }

        runCatching {
            if (legacyRoot.listFiles().isNullOrEmpty()) legacyRoot.delete()
        }
    }

    private fun copyAndSync(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
    }

    private fun readEntry(directory: File): SafeTrashEntry? {
        val root = runCatching { trashDirectory.canonicalFile }.getOrNull() ?: return null
        val candidate = runCatching { directory.canonicalFile }.getOrNull() ?: return null
        val rootPrefix = root.path.trimEnd(File.separatorChar) + File.separator
        if (candidate == root || !candidate.path.startsWith(rootPrefix)) return null

        val metadata = File(candidate, METADATA_NAME)
        val payload = File(candidate, PAYLOAD_NAME)
        if (!metadata.isFile || !payload.isFile) return null

        val properties = runCatching {
            Properties().apply {
                metadata.inputStream().use { input -> load(input) }
            }
        }.getOrNull() ?: return null

        val originalPath = properties.getProperty(KEY_ORIGINAL_PATH)?.takeIf { it.isNotBlank() }
            ?: return null
        val originalName = properties.getProperty(KEY_ORIGINAL_NAME)?.takeIf { it.isNotBlank() }
            ?: return null
        val sizeBytes = properties.getProperty(KEY_SIZE_BYTES)?.toLongOrNull()?.takeIf { it >= 0L }
            ?: return null
        val lastModified = properties.getProperty(KEY_LAST_MODIFIED)?.toLongOrNull()?.takeIf { it >= 0L }
            ?: return null
        val storedHash = properties.getProperty(KEY_SHA256)
        val hash = when {
            storedHash == null -> {
                // Recovery 0.4 entries predate the stored digest. Establish and
                // persist a one-time baseline from the app-private payload.
                val migratedHash = runCatching { sha256(payload) }.getOrNull() ?: return null
                val persisted = runCatching {
                    properties.setProperty(KEY_SHA256, migratedHash)
                    FileOutputStream(metadata).use { output ->
                        properties.store(output, METADATA_COMMENT)
                        output.fd.sync()
                    }
                    true
                }.getOrDefault(false)
                if (!persisted) return null
                migratedHash
            }
            storedHash.length == 64 &&
                storedHash.all { char -> char in '0'..'9' || char in 'a'..'f' } -> storedHash
            else -> return null
        }

        return SafeTrashEntry(
            id = candidate.name,
            originalPath = originalPath,
            originalName = originalName,
            sizeBytes = sizeBytes,
            lastModified = lastModified,
            sha256 = hash,
        )
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
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
        private const val KEY_SHA256 = "sha256"
        private val ENTRY_ID_PATTERN =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        internal fun forTests(
            trashDirectory: File,
            restoreRoot: File = trashDirectory.parentFile ?: trashDirectory,
            legacyTrashDirectory: File? = null,
        ): SafeTrashManager = SafeTrashManager(
            trashDirectory = trashDirectory,
            restoreRoot = restoreRoot,
            legacyTrashDirectory = legacyTrashDirectory,
        )
    }
}
