package com.example.airsense.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class PreferenceHelper @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore

    private companion object {
        val FDS_MODE = stringPreferencesKey("fds_mode")
        val OVERRIDE_THEME_MODE = booleanPreferencesKey("override_theme_mode")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val CAN_NOTIFY = booleanPreferencesKey("can_notify")
    }

    val fdsMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[FDS_MODE] ?: "Primary"
    }

    val overrideThemeMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[OVERRIDE_THEME_MODE] ?: false
    }

    val isDarkTheme: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_DARK_THEME] ?: false
    }

    val canNotify: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[CAN_NOTIFY] ?: true
    }

    suspend fun setFDSMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[FDS_MODE] = mode
        }
    }

    suspend fun setOverrideThemeMode(override: Boolean) {
        dataStore.edit { preferences ->
            preferences[OVERRIDE_THEME_MODE] = override
        }
    }

    suspend fun setIsDarkTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_DARK_THEME] = isDark
        }
    }

    suspend fun setCanNotify(canNotify: Boolean) {
        dataStore.edit { preferences ->
            preferences[CAN_NOTIFY] = canNotify
        }
    }
}