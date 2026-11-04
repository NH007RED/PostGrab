package com.postgrab.app.presentation.downloads

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postgrab.app.domain.model.SavedImage
import com.postgrab.app.domain.repository.ImageRepository
import com.postgrab.app.domain.usecase.DeleteSavedImagesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DownloadsUiState(
    val items: List<SavedImage> = emptyList(),
    val loading: Boolean = true,
    val selected: Set<Uri> = emptySet()
)

class DownloadsViewModel(
    private val repo: ImageRepository,
    private val deleteUseCase: DeleteSavedImagesUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(DownloadsUiState())
    val state: StateFlow<DownloadsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { runCatching { repo.listSaved() }.getOrDefault(emptyList()) }
            _state.update { st ->
                st.copy(items = list, loading = false, selected = st.selected.filter { u -> list.any { it.uri == u } }.toSet())
            }
        }
    }

    fun toggle(uri: Uri) = _state.update {
        it.copy(selected = if (uri in it.selected) it.selected - uri else it.selected + uri)
    }
    fun selectAll() = _state.update { it.copy(selected = it.items.map { i -> i.uri }.toSet()) }
    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }

    /** Returns URIs that need the system delete confirmation (Android 11+). */
    suspend fun delete(uris: List<Uri>): List<Uri> {
        val failed = deleteUseCase(uris)
        _state.update { it.copy(selected = emptySet()) }
        refresh()
        return failed
    }
}
