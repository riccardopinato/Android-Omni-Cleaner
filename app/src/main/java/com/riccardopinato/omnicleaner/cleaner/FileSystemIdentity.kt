package com.riccardopinato.omnicleaner.cleaner

import android.os.Build
import android.system.Os
import android.system.OsConstants
import java.io.File

data class PhysicalFileIdentity(
    val deviceId: Long,
    val inode: Long,
)

data class StrongFileRevision(
    val physicalIdentity: PhysicalFileIdentity,
    val changeTimeSeconds: Long,
    val changeTimeNanos: Long,
)

internal data class FileSystemEntryMetadata(
    val isDirectory: Boolean,
    val isRegularFile: Boolean,
    val isSymlink: Boolean,
    val sizeBytes: Long,
    val modifiedMillis: Long,
    val physicalIdentity: PhysicalFileIdentity?,
    val revision: StrongFileRevision?,
)

internal object FileSystemIdentity {
    fun read(file: File): FileSystemEntryMetadata? {
        val androidStat = runCatching {
            val stat = Os.lstat(file.absolutePath)
            val mode = stat.st_mode
            val physical = PhysicalFileIdentity(
                deviceId = stat.st_dev,
                inode = stat.st_ino,
            )

            val modifiedMillis: Long
            val strongRevision: StrongFileRevision?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                modifiedMillis = timespecToMillis(
                    stat.st_mtim.tv_sec,
                    stat.st_mtim.tv_nsec,
                )
                strongRevision = StrongFileRevision(
                    physicalIdentity = physical,
                    changeTimeSeconds = stat.st_ctim.tv_sec,
                    changeTimeNanos = stat.st_ctim.tv_nsec,
                )
            } else {
                // API 21-26 exposes only second-resolution st_mtime/st_ctime.
                // Keep dev/inode for alias detection, but deliberately refuse
                // persistent derived-hash reuse without a high-resolution
                // change token.
                modifiedMillis = secondsToMillis(stat.st_mtime)
                strongRevision = null
            }

            FileSystemEntryMetadata(
                isDirectory = OsConstants.S_ISDIR(mode),
                isRegularFile = OsConstants.S_ISREG(mode),
                isSymlink = OsConstants.S_ISLNK(mode),
                sizeBytes = stat.st_size.coerceAtLeast(0L),
                modifiedMillis = modifiedMillis,
                physicalIdentity = physical,
                revision = strongRevision,
            )
        }.getOrNull()
        if (androidStat != null) return androidStat

        // Local JVM tests do not provide a real android.system.Os runtime.
        // Fall back to ordinary File metadata, but expose neither physical
        // identity nor a strong revision. This keeps tests portable without
        // weakening production hash-cache trust.
        return runCatching {
            if (!file.exists()) return@runCatching null
            FileSystemEntryMetadata(
                isDirectory = file.isDirectory,
                isRegularFile = file.isFile,
                isSymlink = file.canonicalFile != file.absoluteFile,
                sizeBytes = file.length().coerceAtLeast(0L),
                modifiedMillis = file.lastModified().coerceAtLeast(0L),
                physicalIdentity = null,
                revision = null,
            )
        }.getOrNull()
    }

    private fun secondsToMillis(seconds: Long): Long =
        runCatching { Math.multiplyExact(seconds, 1_000L) }
            .getOrDefault(Long.MAX_VALUE)
            .coerceAtLeast(0L)

    private fun timespecToMillis(seconds: Long, nanos: Long): Long {
        val secondsMillis = secondsToMillis(seconds)
        if (secondsMillis == Long.MAX_VALUE) return Long.MAX_VALUE
        return runCatching {
            Math.addExact(secondsMillis, (nanos / 1_000_000L).coerceAtLeast(0L))
        }.getOrDefault(Long.MAX_VALUE)
    }
}
