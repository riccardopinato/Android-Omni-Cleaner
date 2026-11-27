package com.riccardopinato.omnicleaner.cleaner

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Properties
import java.util.UUID

/**
 * Durable, append-only crash ledger. Records are recovery EVIDENCE, never
 * authority to delete. Each stage has its own synced file and digest.
 * A crash cannot turn an incomplete stage into an approval.
 */
enum class SafeOperationPhase {
    PREPARED, USER_APPROVED, MUTATING, COMMITTED, REJECTED,
    UNKNOWN_NEEDS_REVIEW,
}

data class SafeOperationRecord(
    val id: String,
    val type: String,
    val fingerprint: String,
    val phase: SafeOperationPhase,
    val sequence: Int,
    val createdAtMs: Long,
    val detail: String = "",
)

class SafeOperationsJournal(private val directory: File) {
    companion object {
        private val crossInstanceLock = Any()
        private const val VERSION = "1"
        private val UUID_PATTERN = Regex("[0-9a-fA-F-]{36}")
    }

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun canonical(record: SafeOperationRecord): String =
        listOf(VERSION, record.id, record.type, record.fingerprint,
            record.phase.name, record.sequence.toString(), record.createdAtMs.toString(),
            record.detail,
        ).joinToString("\u001f")

    private fun durableWrite(record: SafeOperationRecord): Boolean {
        if ((!directory.isDirectory && !directory.mkdirs()) ||
            directory.canonicalFile != directory.absoluteFile
        ) return false
        val destination = File(directory, "${record.id}-${record.sequence}.record")
        if (destination.exists()) return false
        val staging = File(directory, ".${record.id}-${record.sequence}-${UUID.randomUUID()}.tmp")
        return try {
            val p = Properties().apply {
                setProperty("version", VERSION)
                setProperty("id", record.id)
                setProperty("type", record.type)
                setProperty("fingerprint", record.fingerprint)
                setProperty("phase", record.phase.name)
                setProperty("sequence", record.sequence.toString())
                setProperty("createdAt", record.createdAtMs.toString())
                setProperty("detail", record.detail)
                setProperty("checksum", digest(canonical(record)))
            }
            FileOutputStream(staging).use { output ->
                p.store(output, "Omni Safe Operations durable event")
                output.fd.sync()
            }
            if (!staging.renameTo(destination)) {
                staging.delete()
                false
            } else true
        } catch (_: Exception) {
            // Unknown staging entries are never used as successful approvals.
            false
        }
    }

    private fun decode(file: File): SafeOperationRecord? = runCatching {
        if (!file.isFile || file.canonicalFile != file.absoluteFile ||
            !file.name.endsWith(".record")
        ) return@runCatching null
        val p = Properties().also { prop ->
            FileInputStream(file).use { input -> prop.load(input) }
        }
        if (p.getProperty("version") != VERSION) return@runCatching null
        val record = SafeOperationRecord(
            id = p.getProperty("id") ?: return@runCatching null,
            type = p.getProperty("type") ?: return@runCatching null,
            fingerprint = p.getProperty("fingerprint") ?: return@runCatching null,
            phase = SafeOperationPhase.valueOf(p.getProperty("phase")),
            sequence = p.getProperty("sequence")?.toIntOrNull() ?: return@runCatching null,
            createdAtMs = p.getProperty("createdAt")?.toLongOrNull() ?: return@runCatching null,
            detail = p.getProperty("detail").orEmpty(),
        )
        if (!UUID_PATTERN.matches(record.id) || record.sequence < 0 ||
            file.name != "${record.id}-${record.sequence}.record" ||
            p.getProperty("checksum") != digest(canonical(record))
        ) return@runCatching null
        record
    }.getOrNull()

    /**
     * A torn, undecodable or unexpected journal file blocks EVERY new
     * mutation. A missing directory is a fresh journal, not corruption.
     */
    fun integrityHealthy(): Boolean {
        if (!directory.exists()) return true
        if (!directory.isDirectory || directory.canonicalFile != directory.absoluteFile) return false
        val files = directory.listFiles() ?: return false
        return files.all { file ->
            file.isFile && file.name.endsWith(".record") && decode(file) != null
        }
    }

    fun begin(type: String, fingerprint: String, detail: String = ""): SafeOperationRecord? =
        synchronized(crossInstanceLock) {
            if (type.isBlank() || fingerprint.length != 64 ||
                detail.length > 350000 || !integrityHealthy()
            ) return@synchronized null
            val record = SafeOperationRecord(
                UUID.randomUUID().toString(), type, fingerprint,
                SafeOperationPhase.PREPARED, 0, System.currentTimeMillis(), detail,
            )
            if (durableWrite(record)) record else null
        }

    fun advance(
        previous: SafeOperationRecord,
        phase: SafeOperationPhase,
        detail: String = previous.detail,
    ): SafeOperationRecord? = synchronized(crossInstanceLock) {
        if (previous.phase in setOf(
            SafeOperationPhase.COMMITTED, SafeOperationPhase.REJECTED,
        )) return@synchronized null
        // Unknown transactions may become verified after a later read-only
        // provider reconciliation; no replay of the original request.
        if (previous.phase == SafeOperationPhase.UNKNOWN_NEEDS_REVIEW &&
            phase != SafeOperationPhase.COMMITTED
        ) return@synchronized null
        if (!integrityHealthy() || latest(previous.id) != previous ||
            detail.length > 350000
        ) return@synchronized null
        val next = previous.copy(
            phase = phase, sequence = previous.sequence + 1, detail = detail,
        )
        if (durableWrite(next)) next else null
    }

    fun latest(id: String): SafeOperationRecord? =
        directory.listFiles()?.asSequence()
            ?.mapNotNull(::decode)
            ?.filter { it.id == id }
            ?.maxByOrNull { it.sequence }

    /** Read-only crash recovery: incomplete writes are reported, NOT retried. */
    fun needsReview(): List<SafeOperationRecord> =
        directory.listFiles().orEmpty().asSequence()
            .mapNotNull(::decode)
            .groupBy { it.id }
            .values.mapNotNull { records -> records.maxByOrNull { it.sequence } }
            .filter { it.phase !in setOf(SafeOperationPhase.COMMITTED, SafeOperationPhase.REJECTED) }
            .sortedBy { it.createdAtMs }

}

/**
 * Single coordinator for approved local moves. A scan is never an approval:
 * UI must reach an explicit confirmation before calling this method.
 *
 * The checked fingerprint is fixed before a durable approval event is written.
 * Unknown/revoked/corrupted journal => no mutation; there is no auto retry.
 */
class DeletionCoordinator(
    private val trash: SafeTrashManager,
    private val journal: SafeOperationsJournal,
) {
    private companion object { val coordinatorMutex = Any() }

    private fun digestManifest(items: List<CleanupFileSnapshot>): String {
        val manifest = items.sortedBy { it.file.absolutePath }
            .joinToString("\n") { s ->
                listOf(s.file.absolutePath, s.expectedBytes.toString(),
                    s.expectedModified.toString(), s.expectedSha256.orEmpty(),
                    s.kind.name, s.requiredDuplicatePeer?.file?.absolutePath.orEmpty(),
                    s.requiredDuplicatePeer?.expectedSha256.orEmpty(),
                ).joinToString("\u001f")
            }
        return MessageDigest.getInstance("SHA-256")
            .digest(manifest.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * Directory removal can only issue rmdir on a freshly rechecked, EMPTY
     * folder. An arriving file makes rmdir fail; no recursive unlink exists.
     */
    fun removeEmptyFoldersApproved(
        folders: List<File>,
        allowedRoot: File,
    ): EmptyFolderCleanupResult = synchronized(coordinatorMutex) {
        val frozen = folders.distinctBy { it.absolutePath }
        if (frozen.isEmpty()) return@synchronized EmptyFolderCleanupResult(0, 0)
        if (frozen.any { !PathSafety.isSafeFolderToDelete(it, allowedRoot) ||
                it.listFiles()?.isEmpty() != true
        }) return@synchronized EmptyFolderCleanupResult(0, frozen.size)
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(frozen.map { it.absolutePath }.sorted().joinToString("\n")
                .toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        var record = journal.begin("EMPTY_DIRECTORY_RMDIR", hash,
            frozen.joinToString("\n") { it.absolutePath },
        ) ?: return@synchronized EmptyFolderCleanupResult(0, frozen.size)
        record = journal.advance(record, SafeOperationPhase.USER_APPROVED)
            ?: return@synchronized EmptyFolderCleanupResult(0, frozen.size)
        record = journal.advance(record, SafeOperationPhase.MUTATING)
            ?: return@synchronized EmptyFolderCleanupResult(0, frozen.size)
        var removed = 0
        var rejected = 0
        frozen.forEach { directory ->
            if (PathSafety.isSafeFolderToDelete(directory, allowedRoot) &&
                directory.listFiles()?.isEmpty() == true && directory.delete()
            ) removed++ else rejected++
        }
        journal.advance(record,
            if (rejected == 0) SafeOperationPhase.COMMITTED
            else SafeOperationPhase.UNKNOWN_NEEDS_REVIEW,
            "removed=$removed;rejected=$rejected",
        )
        EmptyFolderCleanupResult(removed, rejected)
    }

    fun moveApproved(snapshots: List<CleanupFileSnapshot>): SafeTrashExecutionResult =
        synchronized(coordinatorMutex) {
            val unique = strongestCleanupSnapshots(snapshots)
            if (unique.isEmpty()) return@synchronized SafeTrashExecutionResult(0, 0, 0L)
            // Freeze every file and every duplicate survivor before the first move.
            val frozen = unique.mapNotNull(CleanupSnapshotValidator::withCurrentDigest)
            if (frozen.size != unique.size || frozen.any {
                !CleanupSnapshotValidator.validate(it).valid
            }) return@synchronized SafeTrashExecutionResult(0, unique.size, 0L)
            // A duplicate keeper cannot simultaneously be a mutation target,
            // even if another module selected it as a large file.
            val mutationPaths = frozen.map {
                runCatching { it.file.canonicalPath }.getOrElse { it.file.absolutePath }
            }.toSet()
            if (frozen.any { item ->
                item.requiredDuplicatePeer?.let { peer ->
                    val keeperPath = runCatching { peer.file.canonicalPath }
                        .getOrElse { peer.file.absolutePath }
                    // Path strings cannot detect two hard links to the
                    // same inode. Android API26+ supports isSameFile. On 24/25
                    // there is no trustworthy portable inode check, so do not
                    // perform automatic duplicate cleanup.
                    keeperPath in mutationPaths ||
                        (android.os.Build.VERSION.SDK_INT in 1..25) ||
                        frozen.any { target ->
                            runCatching {
                                java.nio.file.Files.isSameFile(
                                    peer.file.toPath(), target.file.toPath(),
                                )
                            }.getOrElse { true }
                        }
                } == true
            }) return@synchronized SafeTrashExecutionResult(0, unique.size, 0L)
            val fingerprint = digestManifest(frozen)
            var cursor = journal.begin(
                "LOCAL_SAFE_TRASH", fingerprint,
                "selected=${frozen.size};bytes=${frozen.sumOf { it.expectedBytes }}",
            ) ?: return@synchronized SafeTrashExecutionResult(0, unique.size, 0L)
            cursor = journal.advance(cursor, SafeOperationPhase.USER_APPROVED)
                ?: return@synchronized SafeTrashExecutionResult(0, unique.size, 0L)
            cursor = journal.advance(cursor, SafeOperationPhase.MUTATING)
                ?: return@synchronized SafeTrashExecutionResult(0, unique.size, 0L)

            var moved = 0
            var movedBytes = 0L
            var rejected = 0
            for (file in frozen) {
                // Invalidated keeper or another vanished file stops the batch;
                // never continue based on an old assumption of survival.
                if (!CleanupSnapshotValidator.validate(file).valid) {
                    rejected += frozen.size - moved - rejected
                    break
                }
                val result = trash.moveToTrash(file)
                if (result != null && result.isFile &&
                    !file.file.exists() &&
                    result.length() == file.expectedBytes
                ) {
                    moved++
                    movedBytes += file.expectedBytes
                } else {
                    rejected += frozen.size - moved - rejected
                    break
                }
            }
            journal.advance(
                cursor,
                if (rejected == 0) SafeOperationPhase.COMMITTED
                else SafeOperationPhase.UNKNOWN_NEEDS_REVIEW,
                "moved=$moved;rejected=$rejected;bytes=$movedBytes",
            )
            SafeTrashExecutionResult(moved, rejected, movedBytes)
        }
}
