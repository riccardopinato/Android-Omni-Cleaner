package com.riccardopinato.omnicleaner.cleaner

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.riccardopinato.omnicleaner.model.GalleryMediaFilter
import com.riccardopinato.omnicleaner.model.GallerySortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.galleryDataStore by preferencesDataStore(name = "gallery_session")

data class GallerySession(
    val anchorId: Long = -1L,
    val index: Int = 0,
    val reviewed: Int = 0,
    val filter: GalleryMediaFilter = GalleryMediaFilter.ALL,
    val sort: GallerySortOrder = GallerySortOrder.DATE_DESC,
)

class GalleryPreferencesManager(private val context: Context) {
    private object Keys {
        val ANCHOR_ID = longPreferencesKey("anchor_id")
        val INDEX = intPreferencesKey("index")
        val REVIEWED = intPreferencesKey("reviewed")
        val FILTER = stringPreferencesKey("filter")
        val SORT = stringPreferencesKey("sort")
    }

    val sessionFlow: Flow<GallerySession> = context.galleryDataStore.data.map { prefs ->
        GallerySession(
            anchorId = prefs[Keys.ANCHOR_ID] ?: -1L,
            index = prefs[Keys.INDEX] ?: 0,
            reviewed = prefs[Keys.REVIEWED] ?: 0,
            filter = runCatching {
                GalleryMediaFilter.valueOf(prefs[Keys.FILTER].orEmpty())
            }.getOrDefault(GalleryMediaFilter.ALL),
            sort = runCatching {
                GallerySortOrder.valueOf(prefs[Keys.SORT].orEmpty())
            }.getOrDefault(GallerySortOrder.DATE_DESC),
        )
    }

    suspend fun saveProgress(anchorId: Long, index: Int, reviewed: Int) {
        context.galleryDataStore.edit { prefs ->
            prefs[Keys.ANCHOR_ID] = anchorId
            prefs[Keys.INDEX] = index
            prefs[Keys.REVIEWED] = reviewed
        }
    }

    suspend fun saveFilterAndSort(filter: GalleryMediaFilter, sort: GallerySortOrder) {
        context.galleryDataStore.edit { prefs ->
            prefs[Keys.FILTER] = filter.name
            prefs[Keys.SORT] = sort.name
        }
    }

    suspend fun resetSession() {
        context.galleryDataStore.edit { it.clear() }
    }
}
