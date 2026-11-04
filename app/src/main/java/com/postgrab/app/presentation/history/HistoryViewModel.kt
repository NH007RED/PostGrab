package com.postgrab.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postgrab.app.domain.model.HistoryItem
import com.postgrab.app.domain.repository.ImageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repo: ImageRepository) : ViewModel() {
    val items: StateFlow<List<HistoryItem>> =
        repo.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recent: StateFlow<List<HistoryItem>> =
        repo.recentDownloads.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clear() { viewModelScope.launch { repo.clearHistory() } }
    fun remove(id: Long) { viewModelScope.launch { repo.deleteHistory(id) } }
}
