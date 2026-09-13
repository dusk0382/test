package com.dusk0382.cecosesolaprecios.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class AppPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyTheme = stringPreferencesKey("theme")
    private val keyUsd = booleanPreferencesKey("usd")

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { prefs ->
            val stored = prefs[keyTheme]
            ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
        }
        .catch { emit(ThemeMode.SYSTEM) }

    val usd: Flow<Boolean> = context.dataStore.data
        .map { it[keyUsd] ?: false }
        .catch { emit(false) }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[keyTheme] = mode.name }
    }

    suspend fun setUsd(enabled: Boolean) {
        context.dataStore.edit { it[keyUsd] = enabled }
    }
}
