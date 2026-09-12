package com.example.randplayer.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.randplayer.domain.model.AppSettings
import com.example.randplayer.domain.model.AppTheme
import com.example.randplayer.domain.model.RandomMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val RANDOM_MODE = stringPreferencesKey("random_mode")
        val SMART_SHUFFLE_LIMIT = intPreferencesKey("smart_shuffle_limit")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val SYNC_INTERVAL = intPreferencesKey("sync_interval")
        val SYNC_WIFI_ONLY = booleanPreferencesKey("sync_wifi_only")
        val SYNC_ON_MOBILE = booleanPreferencesKey("sync_on_mobile")
        val SYNC_WHILE_CHARGING = booleanPreferencesKey("sync_while_charging")
        val SYNC_BEFORE_PLAYBACK = booleanPreferencesKey("sync_before_playback")
        val SUPPORTED_EXTENSIONS = stringSetPreferencesKey("supported_extensions")
        val PREFERRED_PLAYER = stringPreferencesKey("preferred_player")
        val THEME = stringPreferencesKey("theme")
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                randomMode = RandomMode.valueOf(
                    preferences[PreferencesKeys.RANDOM_MODE] ?: RandomMode.UNIFORM.name
                ),
                smartShuffleHistoryLimit = preferences[PreferencesKeys.SMART_SHUFFLE_LIMIT] ?: 50,
                autoSyncEnabled = preferences[PreferencesKeys.AUTO_SYNC] ?: true,
                syncIntervalHours = preferences[PreferencesKeys.SYNC_INTERVAL] ?: 6,
                syncWifiOnly = preferences[PreferencesKeys.SYNC_WIFI_ONLY] ?: true,
                syncOnMobile = preferences[PreferencesKeys.SYNC_ON_MOBILE] ?: false,
                syncWhileCharging = preferences[PreferencesKeys.SYNC_WHILE_CHARGING] ?: false,
                syncBeforePlayback = preferences[PreferencesKeys.SYNC_BEFORE_PLAYBACK] ?: false,
                supportedExtensions = (preferences[PreferencesKeys.SUPPORTED_EXTENSIONS] ?: AppSettings().supportedExtensions.toSet()).toList(),
                preferredPlayerPackage = preferences[PreferencesKeys.PREFERRED_PLAYER],
                theme = AppTheme.valueOf(
                    preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
                )
            )
        }

    suspend fun updateRandomMode(mode: RandomMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.RANDOM_MODE] = mode.name
        }
    }

    suspend fun updateSmartShuffleLimit(limit: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMART_SHUFFLE_LIMIT] = limit
        }
    }

    suspend fun updateAutoSync(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SYNC] = enabled
        }
    }

    suspend fun updateSyncInterval(hours: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_INTERVAL] = hours
        }
    }

    suspend fun updateSyncWifiOnly(wifiOnly: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_WIFI_ONLY] = wifiOnly
        }
    }

    suspend fun updateSyncOnMobile(onMobile: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_ON_MOBILE] = onMobile
        }
    }

    suspend fun updateSyncWhileCharging(whileCharging: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_WHILE_CHARGING] = whileCharging
        }
    }

    suspend fun updateSyncBeforePlayback(beforePlayback: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_BEFORE_PLAYBACK] = beforePlayback
        }
    }

    suspend fun updateSupportedExtensions(extensions: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SUPPORTED_EXTENSIONS] = extensions.toSet()
        }
    }

    suspend fun updatePreferredPlayer(packageName: String?) {
        dataStore.edit { preferences ->
            if (packageName != null) {
                preferences[PreferencesKeys.PREFERRED_PLAYER] = packageName
            } else {
                preferences.remove(PreferencesKeys.PREFERRED_PLAYER)
            }
        }
    }

    suspend fun updateTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }
}
