package com.dusk0382.cecosesolaprecios.data.prefs

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Cuántas veces se reintenta una lectura fallida antes de rendirse. */
private const val REINTENTOS = 3
private const val TAG = "CecoPrefs"

@Singleton
class AppPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyTheme = stringPreferencesKey("theme")
    private val keyUsd = booleanPreferencesKey("usd")

    /**
     * Lectura de preferencias que sobrevive a un error transitorio.
     *
     * La versión anterior era `catch { emit(default) }` sobre cada flow derivado.
     * Eso **termina el flow**: emite un valor y completa normalmente. Como
     * `MainViewModel` lo consume con `stateIn(Eagerly)`, que no lo vuelve a
     * suscribir, un solo error de lectura dejaba el tema y el toggle de USD
     * **muertos por el resto de la vida del proceso**: el usuario tocaba "Oscuro",
     * la escritura iba a disco, y ni la app ni los chips cambiaban. Sin log era
     * indistinguible de un bug. Convertir un error transitorio en una falla
     * permanente es lo contrario de lo que `catch` debería hacer.
     *
     * El reintento es acotado a propósito: si el archivo está corrupto, reintentar
     * no lo repara y un reintento infinito en background no termina nunca. En ese
     * caso cae al `catch`, que al menos deja la app viva con los valores por
     * defecto — y ahora queda registrado en el log, que antes se lo comía en
     * silencio.
     */
    private fun lecturas(): Flow<Preferences> = context.dataStore.data
        .retryWhen { causa, intento ->
            val reintentable = causa is IOException && intento < REINTENTOS
            if (reintentable) {
                Log.w(TAG, "falló la lectura de preferencias: ${causa.message}; reintento ${intento + 1}", causa)
                delay(200L * (intento + 1))
            }
            reintentable
        }
        .catch { causa ->
            Log.e(TAG, "no se pudieron leer las preferencias, se usan los valores por defecto: ${causa.message}", causa)
            emit(emptyPreferences())
        }

    val themeMode: Flow<ThemeMode> = lecturas()
        .map { prefs -> ThemeMode.entries.firstOrNull { it.name == prefs[keyTheme] } ?: ThemeMode.SYSTEM }

    val usd: Flow<Boolean> = lecturas()
        .map { it[keyUsd] ?: false }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[keyTheme] = mode.name }
    }

    suspend fun setUsd(enabled: Boolean) {
        context.dataStore.edit { it[keyUsd] = enabled }
    }
}
