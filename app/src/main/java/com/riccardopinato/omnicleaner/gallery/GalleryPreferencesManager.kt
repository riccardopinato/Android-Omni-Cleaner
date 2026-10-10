package com.riccardopinato.omnicleaner.gallery

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val filter: GalleryMediaFilter = GalleryMediaFilter.ALL,
        val sortOrder: GallerySortOrder = GallerySortOrder.DATE_DESC,
        val totalReviewedCount: Int = 0,
    )

    val sessionFlow: Flow<GallerySessionState> = context.galleryDataStore.data.map { preferences ->
        GallerySessionState(
            lastMediaId = preferences[Keys.LAST_MEDIA_ID] ?: 0L,
            lastMediaIndex = preferences[Keys.LAST_MEDIA_INDEX] ?: 0,
            filter = enumValueOrDefault(
                preferences[Keys.FILTER],
                GalleryMediaFilter.ALL,
            ),
            sortOrder = enumValueOrDefault(
                preferences[Keys.SORT_ORDER],
                GallerySortOrder.DATE_DESC,
            ),
            totalReviewedCount = preferences[Keys.TOTAL_REVIEWED] ?: 0,
        )
    }

    suspend fun saveProgress(
        lastMediaId: Long,
        lastMediaIndex: Int,
        totalReviewedCount: Int,
    ) {
        context.galleryDataStore.edit { preferences ->
            preferences[Keys.LAST_MEDIA_ID] = lastMediaId
            preferences[Keys.LAST_MEDIA_INDEX] = lastMediaIndex
            preferences[Keys.TOTAL_REVIEWED] = totalReviewedCount
        }
    }

    suspend fun saveFilterAndSort(
        filter: GalleryMediaFilter,
        sortOrder: GallerySortOrder,
    ) {
        context.galleryDataStore.edit { preferences ->
            preferences[Keys.FILTER] = filter.name
            preferences[Keys.SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun resetSession() {
        context.galleryDataStore.edit { preferences ->
            preferences.remove(Keys.LAST_MEDIA_ID)
            preferences.remove(Keys.LAST_MEDIA_INDEX)
            preferences.remove(Keys.TOTAL_REVIEWED)
        }
    }

    private object Keys {
        val LAST_MEDIA_ID = longPreferencesKey("last_media_id")
        val LAST_MEDIA_INDEX = intPreferencesKey("last_media_index")
        val FILTER = stringPreferencesKey("filter")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val TOTAL_REVIEWED = intPreferencesKey("total_reviewed_count")
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        raw: String?,
        fallback: T,
    ): T = runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(fallback)
}
