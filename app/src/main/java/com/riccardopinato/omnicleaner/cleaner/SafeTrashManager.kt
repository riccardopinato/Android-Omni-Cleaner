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

/** Unknown entries are never silently purged or hidden as empty storage. */
data class SafeTrashInventory(
    val entries: List<SafeTrashEntry>,
    val totalPayloadBytes: Long,
    val unrecognizedEntries: Int,
    val readable: Boolean,
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

/** A file is reclaimed only when its entire trash entry is no longer present. */
enum class SafeTrashPurgeStatus {
    DELETED,
    ENTRY_NOT_FOUND,
    INVALID_ENTRY,
    TRASH_UNAVAILABLE,
    DELETE_FAILED,
}

data class SafeTrashPurgeResult(
    val status: SafeTrashPurgeStatus,
    val reclaimedBytes: Long = 0L,
) {
    val deleted: Boolean get() = status == SafeTrashPurgeStatus.DELETED
}

data class SafeTrashEmptyResult(
    val deletedEntries: Int,
    val rejectedEntries: Int,
    val reclaimedBytes: Long,
    val completed: Boolean,
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
    }

    fun getTrashDirectory(): File = trashDirectory

    fun isPersistentTrashAvailable(): Boolean =
        trashDirectory.isDirectory &&
            !PathSafety.isSymlink(trashDirectory) &&
            PathSafety.isWithinRoot(trashDirectory, restoreRoot)

    fun inspect(): SafeTrashInventory {
        if (!isPersistentTrashAvailable()) {
            return SafeTrashInventory(emptyList(), 0L, 0, readable = false)
        }
        val children = trashDirectory.listFiles()
            ?: return SafeTrashInventory(emptyList(), 0L, 0, readable = false)

        val valid = ArrayList<SafeTrashEntry>(children.size)
        var bytes = 0L
        var unrecognized = 0
        for (child in children) {
            val payload = File(child, PAYLOAD_NAME)
            if (payload.isFile && !PathSafety.isSymlink(payload)) {
                bytes += payload.length().coerceAtLeast(0L)
            }
            val entry = if (child.isDirectory && !PathSafety.isSymlink(child) &&
                ENTRY_ID_PATTERN.matches(child.name)
            ) readEntry(child) else null
            if (entry == null) unrecognized++ else valid += entry
        }
        valid.sortByDescending { File(trashDirectory, it.id).lastModified() }
        return SafeTrashInventory(valid, bytes, unrecognized, readable = true)
    }

    fun listEntries(): List<SafeTrashEntry> = inspect().entries

    fun getTrashSize(): Long = inspect().totalPayloadBytes

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

    /**
     * Cache-only exception: confirmed thumbnail files, including JPEG/PNG
     * extensions, enter the reversible Safe Trash rather than File.delete().
     * Ordinary media cannot use this bypass. The directory must be a known
     * .thumbnails root, with an immutable scanned SHA-256 snapshot.
     */
    fun moveVerifiedThumbnailToTrash(
        stamp: ThumbnailFileStamp,
        scannedDirectory: File,
    ): File? {
        val root = runCatching { scannedDirectory.canonicalFile }.getOrNull() ?: return null
        if (root != scannedDirectory.absoluteFile ||
            !ThumbnailDirectoryPolicy.isSafeDirectory(root) ||
            !PathSafety.isWithinRoot(root, restoreRoot) ||
            !PathSafety.isWithinRoot(stamp.file, root) ||
            PathSafety.isSymlink(root) ||
            stamp.sha256.length != 64 ||
            !stamp.sha256.all { it in '0'..'9' || it in 'a'..'f' }
        ) return null
        val snapshot = CleanupFileSnapshot(
            file = stamp.file,
            expectedBytes = stamp.bytes,
            expectedModified = stamp.modified,
            expectedSha256 = stamp.sha256,
        )
        // CleanupSnapshotValidator intentionally blocks general Android/media.
        // This narrow cache exception uses the validated directory and its
        // immutable SHA, never granting access to arbitrary app media files.
        if (!stamp.file.isFile || PathSafety.isSymlink(stamp.file) ||
            stamp.file.length() != stamp.bytes ||
            stamp.file.lastModified() != stamp.modified ||
            runCatching { sha256(stamp.file) }.getOrNull() != stamp.sha256
        ) return null
        return moveToTrashInternal(
            file = stamp.file,
            expectedSize = stamp.bytes,
            expectedModified = stamp.modified,
            expectedHash = stamp.sha256,
            cacheMediaException = true,
        )
    }

    private fun moveToTrashInternal(
        file: File,
        expectedSize: Long?,
        expectedModified: Long?,
        expectedHash: String?,
        cacheMediaException: Boolean = false,
    ): File? {
        if (!isPersistentTrashAvailable()) return null
        if (!file.exists() || !file.isFile) return null
        if (!PathSafety.isWithinRoot(file, restoreRoot)) return null
        if ((PathSafety.isDangerousPath(file) && !cacheMediaException) ||
            PathSafety.isSymlink(file)
        ) return null
        if (MediaStoreHelper.isMediaFile(file) && !cacheMediaException) return null

        val originalSize = file.length().coerceAtLeast(0L)
        val originalModified = file.lastModified().coerceAtLeast(0L)
        // Bounded reversible cleanup includes large installers; never delete directly.
        if (originalSize > MAX_REVERSIBLE_TRASH_BYTES) return null
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

            // Never delete the relocated payload unless rollback succeeded:
            // otherwise the only remaining copy could be lost.
            val rolledBack = runCatching {
                !file.exists() && payload.isFile && payload.renameTo(file)
            }.getOrDefault(false)
            if (rolledBack) entry.deleteRecursively()
            // Failed rollback: retain payload plus metadata for recovery.
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
                    input.copyTo(output, bufferSize = FILE_IO_BUFFER_BYTES)
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

        if (!hasOnlyKnownEntryFiles(directory)) {
            return SafeTrashRestoreResult(SafeTrashRestoreStatus.PAYLOAD_INVALID)
        }
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
                    input.copyTo(output, bufferSize = FILE_IO_BUFFER_BYTES)
                    output.fd.sync()
                }
            }
            temp.length() == entry.sizeBytes && sha256(temp) == entry.sha256
        }.getOrDefault(false)

        if (!copied) {
            temp.delete()
            return SafeTrashRestoreResult(SafeTrashRestoreStatus.COPY_FAILED)
        }

        // renameTo() may replace a file created concurrently after exists().
        // A hard link is an atomic no-replace publish on supported filesystems.
        // On Android storage without hard links, reserve with createNewFile()
        // (O_EXCL equivalent) before copying. Never open an existing target.
        val linked = runCatching {
            android.system.Os.link(temp.absolutePath, destination.absolutePath)
            true
        }.getOrDefault(false)
        if (!linked) {
            // Android 24+ (minSdk): O_CREAT|O_EXCL atomically refuses a target
            // that another process created. Do not follow symlinks.
            // Local JVM tests (SDK_INT=0) cannot invoke Android Os and use
            // java.io's createNewFile() only in that non-device test environment.
            val exclusiveCopy = if (android.os.Build.VERSION.SDK_INT >= 24) {
                val fd = runCatching {
                    android.system.Os.open(
                        destination.absolutePath,
                        android.system.OsConstants.O_WRONLY or
                            android.system.OsConstants.O_CREAT or
                            android.system.OsConstants.O_EXCL or
                            android.system.OsConstants.O_NOFOLLOW,
                        0x180,
                    )
                }.getOrNull()
                if (fd == null) {
                    temp.delete()
                    return SafeTrashRestoreResult(
                        if (destination.exists()) SafeTrashRestoreStatus.DESTINATION_EXISTS
                        else SafeTrashRestoreStatus.COPY_FAILED,
                    )
                }
                runCatching {
                    FileInputStream(temp).use { input ->
                        FileOutputStream(fd).use { output ->
                            input.copyTo(output, bufferSize = FILE_IO_BUFFER_BYTES)
                            output.fd.sync()
                        }
                    }
                    destination.length() == entry.sizeBytes &&
                        sha256(destination) == entry.sha256
                }.getOrDefault(false)
            } else {
                if (!runCatching { destination.createNewFile() }.getOrDefault(false)) {
                    temp.delete()
                    return SafeTrashRestoreResult(SafeTrashRestoreStatus.DESTINATION_EXISTS)
                }
                runCatching {
                    FileInputStream(temp).use { input ->
                        FileOutputStream(destination).use { output ->
                            input.copyTo(output, bufferSize = FILE_IO_BUFFER_BYTES)
                            output.fd.sync()
                        }
                    }
                    destination.length() == entry.sizeBytes &&
                        sha256(destination) == entry.sha256
                }.getOrDefault(false)
            }
            if (!exclusiveCopy) {
                temp.delete()
                // Preserve both Safe Trash payload and any partial destination
                // for explicit recovery: never unlink a possibly raced file.
                return SafeTrashRestoreResult(SafeTrashRestoreStatus.COPY_FAILED)
            }
        }
        temp.delete()

        if (
            destination.length() != entry.sizeBytes ||
            runCatching { sha256(destination) }.getOrNull() != entry.sha256
        ) {
            // The verified payload still exists in Safe Trash. Never delete
            // a destination that a concurrent process might have modified.
            return SafeTrashRestoreResult(SafeTrashRestoreStatus.COPY_FAILED)
        }

        if (entry.lastModified > 0L) {
            destination.setLastModified(entry.lastModified)
        }

        // Never delete an unknown child that may have appeared after validation.
        removeKnownEntryFilesOnly(directory)

        return SafeTrashRestoreResult(
            status = SafeTrashRestoreStatus.RESTORED,
            restoredFile = destination,
        )
    }

    /**
     * A permanent delete is explicitly scoped to one known Safe Trash entry ID.
     * Unrecognized directories, links and parent paths are never purged.
     */
    fun purgeEntry(entryId: String): SafeTrashPurgeResult {
        if (!isPersistentTrashAvailable()) {
            return SafeTrashPurgeResult(SafeTrashPurgeStatus.TRASH_UNAVAILABLE)
        }
        if (!ENTRY_ID_PATTERN.matches(entryId)) {
            return SafeTrashPurgeResult(SafeTrashPurgeStatus.INVALID_ENTRY)
        }

        val directory = File(trashDirectory, entryId)
        if (!directory.exists()) {
            return SafeTrashPurgeResult(SafeTrashPurgeStatus.ENTRY_NOT_FOUND)
        }
        if (!directory.isDirectory || PathSafety.isSymlink(directory)) {
            return SafeTrashPurgeResult(SafeTrashPurgeStatus.INVALID_ENTRY)
        }
        if (readEntry(directory) == null || !hasOnlyKnownEntryFiles(directory)) {
            return SafeTrashPurgeResult(SafeTrashPurgeStatus.INVALID_ENTRY)
        }

        val root = runCatching { trashDirectory.canonicalFile }.getOrNull()
            ?: return SafeTrashPurgeResult(SafeTrashPurgeStatus.TRASH_UNAVAILABLE)
        val target = runCatching { directory.canonicalFile }.getOrNull()
            ?: return SafeTrashPurgeResult(SafeTrashPurgeStatus.INVALID_ENTRY)
        if (target.parentFile != root || PathSafety.isSymlink(target)) {
            return SafeTrashPurgeResult(SafeTrashPurgeStatus.INVALID_ENTRY)
        }
        // Permanent purge must validate identity immediately before mutation.
        // A replaced/tampered payload must never be treated as the reviewed file.
        val payload = File(target, PAYLOAD_NAME)
        if (!payload.isFile || PathSafety.isSymlink(payload)) {
            return SafeTrashPurgeResult(SafeTrashPurgeStatus.INVALID_ENTRY)
        }
        val verifiedEntry = readEntry(target)
            ?: return SafeTrashPurgeResult(SafeTrashPurgeStatus.INVALID_ENTRY)
        if (payload.length() != verifiedEntry.sizeBytes ||
            runCatching { sha256(payload) }.getOrNull() != verifiedEntry.sha256
        ) {
            return SafeTrashPurgeResult(SafeTrashPurgeStatus.INVALID_ENTRY)
        }
        val bytesBefore = verifiedEntry.sizeBytes
        // Explicit purge is limited to the two recognized files only.
        // Never traverse or delete unexpected descendants.
        val deleted = removeKnownEntryFilesOnly(target)
        if (!deleted || target.exists()) {
            return SafeTrashPurgeResult(SafeTrashPurgeStatus.DELETE_FAILED)
        }
        // The original file remains untouched; only the verified trash entry
        // was removed. SHA is validated on move/restore, not needed to purge.
        return SafeTrashPurgeResult(SafeTrashPurgeStatus.DELETED, bytesBefore)
    }

    fun purgeAll(): SafeTrashEmptyResult {
        if (!isPersistentTrashAvailable()) {
            return SafeTrashEmptyResult(0, 0, 0L, completed = false)
        }
        // A failed directory listing must never be interpreted as success.
        val children = trashDirectory.listFiles()
            ?: return SafeTrashEmptyResult(0, 0, 0L, completed = false)

        var deletedCount = 0
        var failedCount = 0
        var reclaimedBytes = 0L
        for (child in children) {
            val result = purgeEntry(child.name)
            if (result.deleted) {
                deletedCount++
                reclaimedBytes += result.reclaimedBytes
            } else {
                failedCount++
            }
        }
        val actuallyEmpty = trashDirectory.listFiles()?.isEmpty() == true
        return SafeTrashEmptyResult(
            deletedEntries = deletedCount,
            rejectedEntries = failedCount,
            reclaimedBytes = reclaimedBytes,
            completed = failedCount == 0 && actuallyEmpty,
        )
    }

    /** Compatibility path: only immediate children are ever accepted. */
    fun permanentlyDelete(file: File): Boolean =
        file.absoluteFile.parentFile == trashDirectory.absoluteFile &&
            purgeEntry(file.name).deleted

    fun emptyTrash(): Boolean = purgeAll().completed

    fun sameContents(first: File, second: File, expectedBytes: Long): Boolean {
        if (!first.isFile || !second.isFile) return false
        if (first.length() != expectedBytes || second.length() != expectedBytes) return false

        FileInputStream(first).use { left ->
            FileInputStream(second).use { right ->
                val leftBuffer = ByteArray(FILE_IO_BUFFER_BYTES)
                val rightBuffer = ByteArray(FILE_IO_BUFFER_BYTES)
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
    fun migrateLegacyTrashIfNeeded() {
        val legacyRoot = legacyTrashDirectory ?: return
        if (!legacyRoot.isDirectory || !isPersistentTrashAvailable()) return

        for (legacyEntry in legacyRoot.listFiles().orEmpty()) {
            if (!legacyEntry.isDirectory || PathSafety.isSymlink(legacyEntry) ||
                !ENTRY_ID_PATTERN.matches(legacyEntry.name) ||
                !hasOnlyKnownEntryFiles(legacyEntry)
            ) continue

            val legacyMetadata = File(legacyEntry, METADATA_NAME)
            val legacyPayload = File(legacyEntry, PAYLOAD_NAME)
            if (
                !legacyMetadata.isFile ||
                !legacyPayload.isFile ||
                PathSafety.isSymlink(legacyPayload)
            ) {
                continue
            }

            val preferredTarget = File(trashDirectory, legacyEntry.name)
            if (
                preferredTarget.exists() &&
                isVerifiedMigrationCopy(legacyPayload, preferredTarget)
            ) {
                removeKnownEntryFilesOnly(legacyEntry)
                continue
            }
            // A collision may contain user data: never delete it.
            // Use a fresh ID when an incomplete or different entry exists.
            val target = if (preferredTarget.exists()) {
                File(trashDirectory, UUID.randomUUID().toString())
            } else {
                preferredTarget
            }
            if (!target.mkdirs()) continue

            val targetMetadata = File(target, METADATA_NAME)
            val targetPayload = File(target, PAYLOAD_NAME)
            val copied = runCatching {
                copyAndSync(legacyMetadata, targetMetadata)
                copyAndSync(legacyPayload, targetPayload)
                true
            }.getOrDefault(false)

            if (!copied || !isVerifiedMigrationCopy(legacyPayload, target)) {
                // Leave an incomplete new copy as an observable unknown entry,
                // rather than recursively deleting any unexpected file.
                // The verified legacy source remains untouched for a retry.
                continue
            }

            removeKnownEntryFilesOnly(legacyEntry)
        }

        runCatching {
            if (legacyRoot.listFiles()?.isEmpty() == true) legacyRoot.delete()
        }
    }

    private fun isVerifiedMigrationCopy(
        legacyPayload: File,
        target: File,
    ): Boolean {
        if (!legacyPayload.isFile || PathSafety.isSymlink(legacyPayload) ||
            !hasOnlyKnownEntryFiles(legacyPayload.parentFile ?: return false) ||
            !hasOnlyKnownEntryFiles(target)
        ) return false

        val migratedEntry = readEntry(target) ?: return false
        // Equal payload bytes do not prove that the migration belongs to the
        // same original file. Never discard legacy metadata on a UUID collision
        // whose destination path or other identity fields differ.
        val legacyProperties = runCatching {
            Properties().apply {
                File(legacyPayload.parentFile, METADATA_NAME).inputStream().use { input -> load(input) }
            }
        }.getOrNull() ?: return false
        if (legacyProperties.getProperty(KEY_ORIGINAL_PATH) != migratedEntry.originalPath ||
            legacyProperties.getProperty(KEY_ORIGINAL_NAME) != migratedEntry.originalName ||
            legacyProperties.getProperty(KEY_SIZE_BYTES)?.toLongOrNull() != migratedEntry.sizeBytes ||
            legacyProperties.getProperty(KEY_LAST_MODIFIED)?.toLongOrNull() != migratedEntry.lastModified
        ) return false
        val targetPayload = File(target, PAYLOAD_NAME)
        if (!targetPayload.isFile || PathSafety.isSymlink(targetPayload)) return false
        if (targetPayload.length() != legacyPayload.length()) return false

        val sourceHash = runCatching { sha256(legacyPayload) }.getOrNull() ?: return false
        val legacyClaimedHash = legacyProperties.getProperty(KEY_SHA256)
        if (legacyClaimedHash != null && legacyClaimedHash != sourceHash) return false
        val targetHash = runCatching { sha256(targetPayload) }.getOrNull() ?: return false

        return targetHash == sourceHash && targetHash == migratedEntry.sha256
    }

    /**
     * Trash entries are defined by exactly two regular files. Unknown children
     * could contain valuable user data and must never be removed by migration,
     * restore cleanup or bulk purge. A failed listing is also a hard stop.
     */
    private fun hasOnlyKnownEntryFiles(directory: File): Boolean {
        if (!directory.isDirectory || PathSafety.isSymlink(directory)) return false
        val children = directory.listFiles() ?: return false
        return children.size == 2 &&
            children.map { it.name }.toSet() == setOf(PAYLOAD_NAME, METADATA_NAME) &&
            children.all { it.isFile && !PathSafety.isSymlink(it) }
    }

    /** Delete only verified, explicitly named entry files; never recurse. */
    private fun removeKnownEntryFilesOnly(directory: File): Boolean {
        if (!hasOnlyKnownEntryFiles(directory)) return false
        val payload = File(directory, PAYLOAD_NAME)
        val metadata = File(directory, METADATA_NAME)
        if (!payload.delete()) return false
        if (!metadata.delete()) return false
        return directory.delete()
    }

    private fun copyAndSync(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output, bufferSize = FILE_IO_BUFFER_BYTES)
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
            val buffer = ByteArray(FILE_IO_BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    internal companion object {
        // Supports common APKs without unbounded cross-volume copy fallback.
        const val MAX_REVERSIBLE_TRASH_BYTES: Long = 512L * 1024L * 1024L
        private const val FILE_IO_BUFFER_BYTES = 256 * 1024
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
