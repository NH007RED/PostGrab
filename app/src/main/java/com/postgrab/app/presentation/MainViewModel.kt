package com.postgrab.app.presentation

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postgrab.app.domain.model.*
import com.postgrab.app.domain.repository.ImageRepository
import com.postgrab.app.domain.usecase.ImportImageUseCase
import com.postgrab.app.domain.usecase.SaveImageUseCase
import com.postgrab.app.utils.ErrorMessages
import com.postgrab.app.utils.UrlCheck
import com.postgrab.app.utils.UrlValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ImportState {
    object Idle : ImportState
    data class Loading(val progress: Float?, val label: String) : ImportState
    object Ready : ImportState
    data class Error(val message: String) : ImportState
}

sealed interface SaveState {
    object Idle : SaveState
    data class Saving(val progress: Float, val label: String) : SaveState
    data class Saved(val name: String) : SaveState
    data class Duplicate(val name: String) : SaveState
    data class Failed(val message: String) : SaveState
}

data class MainUiState(
    val url: String = "",
    val importState: ImportState = ImportState.Idle,
    val images: List<ImportedImage> = emptyList(),
    val index: Int = 0,
    val save: SaveState = SaveState.Idle
)

sealed interface NavEvent { object ToProgress : NavEvent }

class MainViewModel(
    private val repo: ImageRepository,
    private val importUseCase: ImportImageUseCase,
    private val saveUseCase: SaveImageUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val _events = Channel<NavEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var importJob: Job? = null
    private var saveJob: Job? = null
    private val savedIds = mutableSetOf<Long>()

    fun onUrlChange(v: String) = _state.update { it.copy(url = v) }
    fun selectIndex(i: Int) = _state.update { if (i in it.images.indices) it.copy(index = i) else it }
    fun consumeSave() = _state.update { it.copy(save = SaveState.Idle) }

    // ---------- Import ----------

    fun analyzeUrl() {
        when (val check = UrlValidator.check(_state.value.url)) {
            is UrlCheck.Invalid -> {
                importJob?.cancel()
                _state.update { it.copy(importState = ImportState.Error(check.message), images = emptyList()) }
                _events.trySend(NavEvent.ToProgress)
            }
            is UrlCheck.Ok -> runImport("Downloading image…") { onProgress ->
                importUseCase.fromUrl(check.url, onProgress)
            }
        }
    }

    fun importUris(uris: List<Uri>, type: SourceType) {
        if (uris.isEmpty()) return
        runImport(if (uris.size == 1) "Reading image…" else "Reading ${uris.size} images…") {
            importUseCase.fromUris(uris, type)
        }
    }

    fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val stream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (stream != null && intent.type?.startsWith("image/") == true) {
                    importUris(listOf(stream), SourceType.SHARE)
                } else {
                    val url = intent.getStringExtra(Intent.EXTRA_TEXT)?.let { Regex("https?://\\S+").find(it)?.value }
                    if (url != null) { onUrlChange(url); analyzeUrl() }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val list = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (!list.isNullOrEmpty()) importUris(list, SourceType.SHARE)
            }
        }
    }

    fun retry(item: HistoryItem) {
        viewModelScope.launch { repo.deleteHistory(item.id) }
        if (item.sourceType == SourceType.URL) {
            onUrlChange(item.source); analyzeUrl()
        } else {
            importUris(listOf(Uri.parse(item.source)), item.sourceType)
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        _state.update { it.copy(importState = ImportState.Idle) }
    }

    private fun runImport(label: String, block: suspend ((Float) -> Unit) -> List<ImportedImage>) {
        importJob?.cancel()
        saveJob?.cancel()
        savedIds.clear()
        _state.update { it.copy(importState = ImportState.Loading(null, label), images = emptyList(), index = 0, save = SaveState.Idle) }
        _events.trySend(NavEvent.ToProgress)
        importJob = viewModelScope.launch {
            try {
                val imgs = block { p ->
                    _state.update { s ->
                        val cur = s.importState
                        if (cur is ImportState.Loading) s.copy(importState = cur.copy(progress = if (p < 0) null else p)) else s
                    }
                }
                _state.update { it.copy(images = imgs, index = 0, importState = ImportState.Ready) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(importState = ImportState.Error(ErrorMessages.of(e))) }
            }
        }
    }

    // ---------- Save ----------

    fun download(all: Boolean = false, allowDuplicate: Boolean = false) {
        val s = _state.value
        val targets = if (all) s.images.filter { it.historyId !in savedIds }
        else listOfNotNull(s.images.getOrNull(s.index))
        if (targets.isEmpty()) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            var saved = 0
            var lastName = ""
            try {
                for ((i, img) in targets.withIndex()) {
                    val label = if (targets.size > 1) "Saving ${i + 1} of ${targets.size}" else "Saving image"
                    _state.update { it.copy(save = SaveState.Saving(0f, label)) }
                    when (val r = saveUseCase(img, allowDuplicate) { p ->
                        _state.update { st -> st.copy(save = SaveState.Saving(p, label)) }
                    }) {
                        is SaveResult.Saved -> { saved++; lastName = r.name; savedIds += img.historyId }
                        is SaveResult.Duplicate -> {
                            _state.update { it.copy(save = SaveState.Duplicate(r.name)) }
                            return@launch
                        }
                    }
                }
                _state.update { it.copy(save = SaveState.Saved(if (saved == 1) lastName else "$saved images")) }
            } catch (e: CancellationException) {
                _state.update { it.copy(save = SaveState.Idle) }
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(save = SaveState.Failed(ErrorMessages.of(e))) }
            }
        }
    }

    fun cancelSave() {
        saveJob?.cancel()
        _state.update { it.copy(save = SaveState.Idle) }
    }

    override fun onCleared() {
        // Temporary files are cleaned up on next app start.
        super.onCleared()
    }
}
