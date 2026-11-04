package com.postgrab.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.postgrab.app.AppContainer
import com.postgrab.app.domain.usecase.DeleteSavedImagesUseCase
import com.postgrab.app.domain.usecase.ImportImageUseCase
import com.postgrab.app.domain.usecase.SaveImageUseCase
import com.postgrab.app.presentation.downloads.DownloadsViewModel
import com.postgrab.app.presentation.history.HistoryViewModel
import com.postgrab.app.presentation.settings.SettingsViewModel

class VmFactory(private val c: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(MainViewModel::class.java) ->
            MainViewModel(c.repository, ImportImageUseCase(c.repository), SaveImageUseCase(c.repository)) as T
        modelClass.isAssignableFrom(DownloadsViewModel::class.java) ->
            DownloadsViewModel(c.repository, DeleteSavedImagesUseCase(c.repository)) as T
        modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(c.repository) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(c.settingsStore) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
