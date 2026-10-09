package com.riccardopinato.omnicleaner.cleaner

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.riccardopinato.omnicleaner.model.ProScanType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.proDataStore by preferencesDataStore(name = "pro_preferences")

class ProPreferencesManager(private val context: Context) {
    private object Keys {
        val SELECTED_TYPES = stringSetPreferencesKey("selected_types")
    }

    val selectedTypesFlow: Flow<Set<ProScanType>> = context.proDataStore.data.map { prefs ->
        val names = prefs[Keys.SELECTED_TYPES]
        if (names.isNullOrEmpty()) {
            ProScanType.entries.toSet()
        } else {
            names.mapNotNullTo(linkedSetOf()) { name ->
                runCatching { ProScanType.valueOf(name) }.getOrNull()
            }
        }
    }

    suspend fun saveSelectedTypes(types: Set<ProScanType>) {
        context.proDataStore.edit { prefs ->
            prefs[Keys.SELECTED_TYPES] = types.mapTo(linkedSetOf()) { it.name }
        }
    }
}
