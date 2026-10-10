package com.riccardopinato.omnicleaner.cleaner

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

enum class CleanupCandidateKind {
    LARGE_FILE,
    DUPLICATE,
    LEGACY,
}

data class CleanupPeerSnapshot(
    val file: File,
    val expectedBytes: Long,
    val expectedModified: Long,
    val expectedSha256: String,
)

data class CleanupFileSnapshot(
    val file: File,
    val expectedBytes: Long,
    val expectedModified: Long,
    val expectedSha256: String? = null,
    val kind: CleanupCandidateKind = CleanupCandidateKind.LEGACY,
    val requiredDuplicatePeer: CleanupPeerSnapshot? = null,
)

enum class CleanupSnapshotRejection {
    MISSING_OR_NOT_FILE,
    UNSAFE_PATH,
    SYMLINK,
    CHANGED_SINCE_SCAN,
    DIGEST_MISMATCH,
    DUPLICATE_KEEPER_INVALID,
}

data class CleanupSnapshotValidation(
    val valid: Boolean,
    val rejection: CleanupSnapshotRejection? = null,
)

object CleanupSnapshotValidator {
    fun validate(snapshot: CleanupFileSnapshot): CleanupSnapshotValidation {
        val file = snapshot.file
        if (!file.exists() || !file.isFile) {
            return CleanupSnapshotValidation(false, CleanupSnapshotRejection.MISSING_OR_NOT_FILE)
        }
        if (PathSafety.isDangerousPath(file)) {
            return CleanupSnapshotValidation(false, CleanupSnapshotRejection.UNSAFE_PATH)
        }
        if (PathSafety.isSymlink(file)) {
            return CleanupSnapshotValidation(false, CleanupSnapshotRejection.SYMLINK)
        }
        if (
            file.length() != snapshot.expectedBytes ||
            file.lastModified() != snapshot.expectedModified
        ) {
            return CleanupSnapshotValidation(false, CleanupSnapshotRejection.CHANGED_SINCE_SCAN)
        }

        val expectedHash = snapshot.expectedSha256
        if (expectedHash != null) {
            val currentHash = runCatching { sha256(file) }.getOrNull()
            if (currentHash == null || currentHash != expectedHash) {
                return CleanupSnapshotValidation(false, CleanupSnapshotRejection.DIGEST_MISMATCH)
            }
        }

        if (snapshot.kind == CleanupCandidateKind.DUPLICATE) {
            val peer = snapshot.requiredDuplicatePeer
                ?: return CleanupSnapshotValidation(false, CleanupSnapshotRejection.DUPLICATE_KEEPER_INVALID)

            if (
                !peer.file.exists() ||
                !peer.file.isFile ||
                PathSafety.isDangerousPath(peer.file) ||
                PathSafety.isSymlink(peer.file) ||
                peer.file.length() != peer.expectedBytes ||
                peer.file.lastModified() != peer.expectedModified
            ) {
                return CleanupSnapshotValidation(false, CleanupSnapshotRejection.DUPLICATE_KEEPER_INVALID)
            }

            val peerHash = runCatching { sha256(peer.file) }.getOrNull()
            if (peerHash == null || peerHash != peer.expectedSha256 || peerHash != expectedHash) {
                return CleanupSnapshotValidation(false, CleanupSnapshotRejection.DUPLICATE_KEEPER_INVALID)
            }
        }

        return CleanupSnapshotValidation(true)
    }

    fun withCurrentDigest(snapshot: CleanupFileSnapshot): CleanupFileSnapshot? {
        if (!validate(snapshot).valid) return null
        val file = snapshot.file
        val digest = runCatching { sha256(file) }.getOrNull() ?: return null
        if (
            !file.isFile ||
            file.length() != snapshot.expectedBytes ||
            file.lastModified() != snapshot.expectedModified
        ) {
            return null
        }
        if (snapshot.expectedSha256 != null && snapshot.expectedSha256 != digest) {
            return null
        }
        return snapshot.copy(expectedSha256 = snapshot.expectedSha256 ?: digest)
    }

    internal fun currentSha256(file: File): String = sha256(file)

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
}


internal fun strongestCleanupSnapshots(
    snapshots: List<CleanupFileSnapshot>,
): List<CleanupFileSnapshot> {
    val byPath = linkedMapOf<String, CleanupFileSnapshot>()

    for (snapshot in snapshots) {
        val key = runCatching { snapshot.file.canonicalPath }
            .getOrElse { snapshot.file.absolutePath }
        val current = byPath[key]
        if (current == null || snapshot.kind.safetyRank() > current.kind.safetyRank()) {
            byPath[key] = snapshot
        }
    }

    return byPath.values.toList()
}

private fun CleanupCandidateKind.safetyRank(): Int = when (this) {
    CleanupCandidateKind.LEGACY -> 0
    CleanupCandidateKind.LARGE_FILE -> 1
    CleanupCandidateKind.DUPLICATE -> 2
}
