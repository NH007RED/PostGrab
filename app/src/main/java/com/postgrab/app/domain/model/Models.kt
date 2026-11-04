package com.postgrab.app.domain.model

import android.net.Uri
import java.io.File

enum class DownloadStatus { IMPORTED, DOWNLOADED, FAILED }
enum class SourceType { URL, PICKER, FILE, SHARE }
enum class OutputFormat { ORIGINAL, JPEG, PNG, WEBP }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ImportedImage(
    val file: File,
    val source: String,
    val sourceType: SourceType,
    val name: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val mime: String,
    val historyId: Long
)

data class SavedImage(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val dateMs: Long,
    val mime: String
)

data class HistoryItem(
    val id: Long,
    val source: String,
    val sourceType: SourceType,
    val name: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mime: String,
    val status: DownloadStatus,
    val savedUri: String?,
    val error: String?,
    val createdAt: Long
)

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val format: OutputFormat = OutputFormat.ORIGINAL,
    val quality: Int = 90
)

sealed class SaveResult {
    data class Saved(val uri: Uri, val name: String, val mime: String) : SaveResult()
    data class Duplicate(val name: String) : SaveResult()
}

class ImportException(message: String) : Exception(message)
class InsufficientStorageException : Exception("Not enough storage space to save this image.")
