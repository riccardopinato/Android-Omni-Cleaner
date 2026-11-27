package com.riccardopinato.omnicleaner.cleaner

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.MediaStore
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Last completed read-only inventory. Counts here never authorize deletion:
 * every destructive operation revalidates the live file and keeper.
 */
internal data class SavedIndexSummary(
    val rootPath: String = "",
    val fileCount: Int = 0,
    val indexedBytes: Long = 0L,
    val completedAtMillis: Long = 0L,
    val checkedAtMillis: Long = 0L,
    val mediaVersion: String? = null,
    val mediaGeneration: Long? = null,
    val dirty: Boolean = false,
)

internal data class MediaRevisionToken(val version: String, val generation: Long)

internal const val INDEX_FULL_AUDIT_MS: Long = 24L * 60L * 60L * 1000L

/** MediaStore generation covers media; periodic full audits cover other files. */
internal fun shouldRebuildIndex(
    saved: SavedIndexSummary,
    rootPath: String,
    current: MediaRevisionToken?,
    nowMillis: Long,
): Boolean {
    if (saved.dirty || saved.rootPath != rootPath || saved.completedAtMillis <= 0L) return true
    if (nowMillis < saved.completedAtMillis ||
        nowMillis - saved.completedAtMillis >= INDEX_FULL_AUDIT_MS
    ) return true
    if (current != null && saved.mediaVersion != null && saved.mediaGeneration != null) {
        return current.version != saved.mediaVersion ||
            current.generation != saved.mediaGeneration
    }
    return false
}

internal class BackgroundIndexStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        "omni_background_index_v1", Context.MODE_PRIVATE,
    )

    fun isEnabled(): Boolean = prefs.getBoolean("enabled", true)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enabled", enabled).apply()
        signalChange()
    }

    fun read(): SavedIndexSummary = SavedIndexSummary(
        rootPath = prefs.getString("root", "").orEmpty(),
        fileCount = prefs.getInt("files", 0).coerceAtLeast(0),
        indexedBytes = prefs.getLong("bytes", 0L).coerceAtLeast(0L),
        completedAtMillis = prefs.getLong("completed", 0L),
        checkedAtMillis = prefs.getLong("checked", 0L),
        mediaVersion = prefs.getString("media_version", null),
        mediaGeneration = if (prefs.contains("media_gen")) prefs.getLong("media_gen", 0L) else null,
        dirty = prefs.getBoolean("dirty", false),
    )

    fun markDirty() {
        prefs.edit().putBoolean("dirty", true).apply()
        signalChange()
    }

    fun recordChecked(nowMillis: Long) {
        prefs.edit().putLong("checked", nowMillis).apply()
        signalChange()
    }

    fun recordComplete(
        root: File,
        inventory: FileInventory,
        completedAtMillis: Long,
        media: MediaRevisionToken?,
    ) {
        // Only a completed repository snapshot may update these values.
        val total = inventory.files.sumOf { it.size.coerceAtLeast(0L) }
        prefs.edit()
            .putString("root", root.absolutePath)
            .putInt("files", inventory.files.size)
            .putLong("bytes", total)
            .putLong("completed", completedAtMillis)
            .putLong("checked", completedAtMillis)
            .putString("media_version", media?.version)
            .apply {
                if (media == null) remove("media_gen") else putLong("media_gen", media.generation)
            }
            .putBoolean("dirty", false)
            .apply()
        signalChange()
    }

    fun shouldEnqueueCheck(nowMillis: Long): Boolean =
        nowMillis - prefs.getLong("last_enqueued", 0L) >= 5L * 60L * 1000L

    fun markCheckEnqueued(nowMillis: Long) {
        prefs.edit().putLong("last_enqueued", nowMillis).apply()
    }

    companion object {
        private val revision = MutableStateFlow(0L)

        fun mediaRevision(context: Context): MediaRevisionToken? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
            return runCatching {
                val volume = MediaStore.VOLUME_EXTERNAL_PRIMARY
                MediaRevisionToken(
                    version = MediaStore.getVersion(context, volume),
                    generation = MediaStore.getGeneration(context, volume),
                )
            }.getOrNull()
        }
    }

    /** Emits persisted summaries from all repository, worker and UI instances. */
    fun observe(): Flow<SavedIndexSummary> = revision.map { read() }

    private fun signalChange() {
        revision.update { it + 1L }
    }

}
