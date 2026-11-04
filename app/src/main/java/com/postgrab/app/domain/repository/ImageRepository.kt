package com.postgrab.app.domain.repository

import android.net.Uri
import com.postgrab.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ImageRepository {
    val history: Flow<List<HistoryItem>>
    val recentDownloads: Flow<List<HistoryItem>>

    /** onProgress: 0..1, or a negative value when the total size is unknown. */
    suspend fun importUrl(url: String, onProgress: (Float) -> Unit): ImportedImage
    suspend fun importUris(uris: List<Uri>, type: SourceType): List<ImportedImage>
    suspend fun save(image: ImportedImage, allowDuplicate: Boolean, onProgress: (Float) -> Unit): SaveResult

    suspend fun deleteHistory(id: Long)
    suspend fun clearHistory()

    fun listSaved(): List<SavedImage>
    /** Returns the URIs that could not be deleted directly (need user confirmation on Android 11+). */
    suspend fun deleteSaved(uris: List<Uri>): List<Uri>
}
