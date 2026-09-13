package com.dusk0382.cecosesolaprecios.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dusk0382.cecosesolaprecios.data.prefs.AppPrefs
import com.dusk0382.cecosesolaprecios.data.prefs.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prefs: AppPrefs,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /** true = mostrar precios en USD (CEC) cuando la API oficial los aportó. */
    val usd: StateFlow<Boolean> = prefs.usd
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { prefs.setTheme(mode) }
    fun setUsd(enabled: Boolean) = viewModelScope.launch { prefs.setUsd(enabled) }
}
