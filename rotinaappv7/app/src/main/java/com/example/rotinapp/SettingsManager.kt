package com.example.rotinapp

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
}

class SettingsManager(private val context: Context) {

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SettingsKeys.DARK_MODE] ?: false
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[SettingsKeys.DARK_MODE] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled }
    }
}
