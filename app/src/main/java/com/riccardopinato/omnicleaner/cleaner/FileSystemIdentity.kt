package com.riccardopinato.omnicleaner.cleaner

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
    val revision: StrongFileRevision?,
)

internal object FileSystemIdentity {
    fun read(file: File): FileSystemEntryMetadata? = runCatching {
        val stat = Os.lstat(file.absolutePath)
        val mode = stat.st_mode
        val physical = PhysicalFileIdentity(
            deviceId = stat.st_dev,
            inode = stat.st_ino,
        )
        FileSystemEntryMetadata(
            isDirectory = OsConstants.S_ISDIR(mode),
            isRegularFile = OsConstants.S_ISREG(mode),
            isSymlink = OsConstants.S_ISLNK(mode),
            sizeBytes = stat.st_size.coerceAtLeast(0L),
            modifiedMillis = timespecToMillis(stat.st_mtime.tv_sec, stat.st_mtime.tv_nsec),
            revision = StrongFileRevision(
                physicalIdentity = physical,
                changeTimeSeconds = stat.st_ctime.tv_sec,
                changeTimeNanos = stat.st_ctime.tv_nsec,
            ),
        )
    }.getOrNull()

    private fun timespecToMillis(seconds: Long, nanos: Long): Long {
        val secondsMillis = runCatching { Math.multiplyExact(seconds, 1_000L) }
            .getOrDefault(Long.MAX_VALUE)
        if (secondsMillis == Long.MAX_VALUE) return Long.MAX_VALUE
        return (secondsMillis + (nanos / 1_000_000L))
            .coerceAtLeast(0L)
    }
}
