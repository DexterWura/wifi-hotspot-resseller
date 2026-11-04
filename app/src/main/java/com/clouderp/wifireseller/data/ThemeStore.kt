package com.clouderp.wifireseller.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "wifi_reseller_appearance")

enum class ThemePreference {
    System,
    Light,
    Dark,
    ;

    companion object {
        fun fromStored(value: String?): ThemePreference =
            entries.firstOrNull { it.name == value } ?: System
    }
}

class ThemeStore(private val context: Context) {
    private val preferenceKey = stringPreferencesKey("theme_preference")

    val preference: Flow<ThemePreference> = context.themeDataStore.data.map { prefs ->
        ThemePreference.fromStored(prefs[preferenceKey])
    }

    suspend fun setPreference(value: ThemePreference) {
        context.themeDataStore.edit { prefs ->
            prefs[preferenceKey] = value.name
        }
    }
}
