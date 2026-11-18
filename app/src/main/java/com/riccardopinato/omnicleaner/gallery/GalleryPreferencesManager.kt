package com.riccardopinato.omnicleaner.gallery

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.galleryDataStore by preferencesDataStore(name = "gallery_session")

class GalleryPreferencesManager(
    private val context: Context,
) {
    data class GallerySessionState(
        val lastMediaId: Long = 0L,
        val lastMediaIndex: Int = 0,
        val sourceOffset: Int = 0,
        val pendingTrashRefs: Set<String> = emptySet(),
        val filter: GalleryMediaFilter = GalleryMediaFilter.ALL,
        val sortOrder: GallerySortOrder = GallerySortOrder.DATE_DESC,
        val totalReviewedCount: Int = 0,
        val lastUndoRef: String? = null,
    )

    val sessionFlow: Flow<GallerySessionState> = context.galleryDataStore.data.map { preferences ->
        GallerySessionState(
            lastMediaId = preferences[Keys.LAST_MEDIA_ID] ?: 0L,
            lastMediaIndex = preferences[Keys.LAST_MEDIA_INDEX] ?: 0,
            sourceOffset = preferences[Keys.SOURCE_OFFSET] ?: 0,
            pendingTrashRefs = preferences[Keys.PENDING_TRASH_REFS].orEmpty(),
            filter = enumValueOrDefault(
                preferences[Keys.FILTER],
                GalleryMediaFilter.ALL,
            ),
            sortOrder = enumValueOrDefault(
                preferences[Keys.SORT_ORDER],
                GallerySortOrder.DATE_DESC,
            ),
            totalReviewedCount = preferences[Keys.TOTAL_REVIEWED] ?: 0,
            lastUndoRef = preferences[Keys.LAST_UNDO_REF],
        )
    }

    suspend fun saveProgress(
        lastMediaId: Long,
        lastMediaIndex: Int,
        sourceOffset: Int,
        pendingTrashRefs: Set<String>,
        totalReviewedCount: Int,
        lastUndoRef: String? = null,
    ) {
        context.galleryDataStore.edit { preferences ->
            preferences[Keys.LAST_MEDIA_ID] = lastMediaId
            preferences[Keys.LAST_MEDIA_INDEX] = lastMediaIndex
            preferences[Keys.SOURCE_OFFSET] = sourceOffset.coerceAtLeast(0)
            preferences[Keys.PENDING_TRASH_REFS] = pendingTrashRefs
            preferences[Keys.TOTAL_REVIEWED] = totalReviewedCount
            if (lastUndoRef == null) {
                preferences.remove(Keys.LAST_UNDO_REF)
            } else {
                preferences[Keys.LAST_UNDO_REF] = lastUndoRef
            }
        }
    }

    suspend fun saveFilterAndSort(
        filter: GalleryMediaFilter,
        sortOrder: GallerySortOrder,
        pendingTrashRefs: Set<String> = emptySet(),
    ) {
        context.galleryDataStore.edit { preferences ->
            preferences[Keys.FILTER] = filter.name
            preferences[Keys.SORT_ORDER] = sortOrder.name
            // Changing the view resets its cursor, NEVER the explicitly selected
            // Android Trash queue. Keep the queue atomically with filter/sort.
            preferences.remove(Keys.LAST_MEDIA_ID)
            preferences.remove(Keys.LAST_MEDIA_INDEX)
            preferences.remove(Keys.SOURCE_OFFSET)
            preferences[Keys.PENDING_TRASH_REFS] = pendingTrashRefs
            // A saved Undo cursor is only valid in the original sort/filter.
            preferences.remove(Keys.LAST_UNDO_REF)
        }
    }

    suspend fun resetSession() {
        context.galleryDataStore.edit { preferences ->
            preferences.remove(Keys.LAST_MEDIA_ID)
            preferences.remove(Keys.LAST_MEDIA_INDEX)
            preferences.remove(Keys.SOURCE_OFFSET)
            preferences.remove(Keys.PENDING_TRASH_REFS)
            preferences.remove(Keys.TOTAL_REVIEWED)
            preferences.remove(Keys.LAST_UNDO_REF)
        }
    }

    private object Keys {
        val LAST_MEDIA_ID = longPreferencesKey("last_media_id")
        val LAST_MEDIA_INDEX = intPreferencesKey("last_media_index")
        val SOURCE_OFFSET = intPreferencesKey("source_offset")
        val PENDING_TRASH_REFS = stringSetPreferencesKey("pending_trash_refs")
        val FILTER = stringPreferencesKey("filter")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val TOTAL_REVIEWED = intPreferencesKey("total_reviewed_count")
        val LAST_UNDO_REF = stringPreferencesKey("last_undo_ref")
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        raw: String?,
        fallback: T,
    ): T = runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(fallback)
}
