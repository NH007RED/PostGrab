package com.postgrab.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postgrab.app.data.local.SettingsStore
import com.postgrab.app.domain.model.AppSettings
import com.postgrab.app.domain.model.OutputFormat
import com.postgrab.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {
    val settings: StateFlow<AppSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setTheme(t: ThemeMode) { viewModelScope.launch { store.setTheme(t) } }
    fun setDynamic(v: Boolean) { viewModelScope.launch { store.setDynamic(v) } }
    fun setFormat(f: OutputFormat) { viewModelScope.launch { store.setFormat(f) } }
    fun setQuality(q: Int) { viewModelScope.launch { store.setQuality(q) } }
}
