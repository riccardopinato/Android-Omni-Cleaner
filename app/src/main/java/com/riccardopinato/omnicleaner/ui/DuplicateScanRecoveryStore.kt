package com.riccardopinato.omnicleaner.ui

import android.content.Context
import java.io.File

/**
 * The marker records an unfinished request, NOT a trustworthy inventory or
 * scan cursor. On restart the scanner rediscovers the live inventory and may
 * reuse only strongly revision-bound hashes from the separate SQLite index.
 */
internal enum class DuplicateScanMode { REUSE_VERIFIED_INDEX, REFRESH_INVENTORY }

internal data class PendingDuplicateScan(
    val rootPath: String,
    val extension: String?,
    val mode: DuplicateScanMode,
)

internal class DuplicateScanRecoveryStore(
    context: Context,
    name: String = PREFS_NAME,
) {
    private val preferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun pendingFor(root: File): PendingDuplicateScan? {
        if (!preferences.getBoolean(KEY_PENDING, false)) return null
        val path = preferences.getString(KEY_ROOT, null) ?: return null
        if (path != root.absolutePath) return null
        val mode = runCatching {
            DuplicateScanMode.valueOf(preferences.getString(KEY_MODE, null) ?: "")
        }.getOrNull() ?: return null
        val extension = preferences.getString(KEY_EXTENSION, null)
        if (extension != null && !extension.matches(Regex("[a-z0-9]{1,12}"))) return null
        return PendingDuplicateScan(path, extension, mode)
    }

    /** Persist before expensive work; execute on Dispatchers.IO. */
    fun begin(root: File, extension: String?, mode: DuplicateScanMode) {
        require(extension == null || extension.matches(Regex("[a-z0-9]{1,12}")))
        check(
            preferences.edit()
                .putString(KEY_ROOT, root.absolutePath)
                .putString(KEY_EXTENSION, extension)
                .putString(KEY_MODE, mode.name)
                .putBoolean(KEY_PENDING, true)
                .commit()
        ) { "Could not persist duplicate scan recovery marker" }
    }

    /** Clear only the request that actually finished; never another scan. */
    fun complete(root: File, extension: String?, mode: DuplicateScanMode) {
        val pending = pendingFor(root) ?: return
        if (pending.extension != extension || pending.mode != mode) return
        check(preferences.edit().clear().commit()) { "Could not clear duplicate scan recovery marker" }
    }

    private companion object {
        const val PREFS_NAME = "duplicate_scan_recovery_v2"
        const val KEY_PENDING = "pending"
        const val KEY_ROOT = "root"
        const val KEY_EXTENSION = "extension"
        const val KEY_MODE = "mode"
    }
}
