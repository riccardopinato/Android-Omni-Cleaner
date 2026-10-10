package com.riccardopinato.omnicleaner.monetization

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.proDataStore by preferencesDataStore(name = "pro_batch_prefs")

class ProPreferencesManager(private val context: Context) {
    val selectedTypesFlow: Flow<Set<ProScanType>> =
        context.proDataStore.data.map { decode(it[SELECTED_TYPES]) }

    suspend fun saveSelectedTypes(selectedTypes: Set<ProScanType>) {
        context.proDataStore.edit {
            it[SELECTED_TYPES] = selectedTypes.mapTo(linkedSetOf()) { type -> type.name }
        }
    }

    private fun decode(raw: Set<String>?): Set<ProScanType> {
        if (raw == null) return ProScanType.entries.toSet()
        return raw.mapNotNullTo(linkedSetOf()) { name ->
            ProScanType.entries.firstOrNull { it.name == name }
        }
    }

    private companion object {
        val SELECTED_TYPES: Preferences.Key<Set<String>> =
            stringSetPreferencesKey("selected_pro_scan_types")
    }
}
