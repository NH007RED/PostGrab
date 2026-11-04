package com.postgrab.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.postgrab.app.data.local.GalleryStore
import com.postgrab.app.data.local.HistoryDao
import com.postgrab.app.data.local.HistoryEntity
import com.postgrab.app.data.local.SettingsStore
import com.postgrab.app.data.remote.ImageFetcher
import com.postgrab.app.domain.model.*
import com.postgrab.app.domain.repository.ImageRepository
import com.postgrab.app.utils.ErrorMessages
import com.postgrab.app.utils.Formatters
import com.postgrab.app.utils.Notifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageRepositoryImpl(
    private val ctx: Context,
    private val dao: HistoryDao,
    private val fetcher: ImageFetcher,
    private val gallery: GalleryStore,
    private val settings: SettingsStore
) : ImageRepository {

    private val supported = setOf("image/jpeg", "image/png", "image/webp")

    override val history: Flow<List<HistoryItem>> = dao.observeAll().map { l -> l.map { it.toItem() } }
    override val recentDownloads: Flow<List<HistoryItem>> = dao.observeRecent().map { l -> l.map { it.toItem() } }

    // ---------- Import ----------

    override suspend fun importUrl(url: String, onProgress: (Float) -> Unit): ImportedImage =
        withContext(Dispatchers.IO) {
            try {
                val file = fetcher.fetch(url, onProgress)
                try {
                    val (w, h, mime) = inspect(file)
                    val name = nameFromUrl(url, mime)
                    val id = recordImport(url, SourceType.URL, name, file.length(), w, h, mime)
                    ImportedImage(file, url, SourceType.URL, name, w, h, file.length(), mime, id)
                } catch (e: Throwable) { file.delete(); throw e }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                recordFailure(url, SourceType.URL, ErrorMessages.of(e))
                throw e
            }
        }

    override suspend fun importUris(uris: List<Uri>, type: SourceType): List<ImportedImage> =
        withContext(Dispatchers.IO) {
            val ok = mutableListOf<ImportedImage>()
            var firstError: String? = null
            for (uri in uris) {
                val tmp = fetcher.newTempFile()
                try {
                    ctx.contentResolver.openInputStream(uri)?.use { input ->
                        tmp.outputStream().use { out ->
                            val buf = ByteArray(32 * 1024)
                            var total = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                total += n
                                if (total > ImageFetcher.MAX_BYTES) throw ImportException("This image is too large (over 40 MB).")
                                out.write(buf, 0, n)
                            }
                        }
                    } ?: throw ImportException("Could not open this file.")
                    val (w, h, mime) = inspect(tmp)
                    val name = displayName(uri, mime)
                    val id = recordImport(uri.toString(), type, name, tmp.length(), w, h, mime)
                    ok += ImportedImage(tmp, uri.toString(), type, name, w, h, tmp.length(), mime, id)
                } catch (e: CancellationException) {
                    tmp.delete(); throw e
                } catch (e: Exception) {
                    tmp.delete()
                    val msg = ErrorMessages.of(e)
                    recordFailure(uri.toString(), type, msg)
                    if (firstError == null) firstError = msg
                }
            }
            if (ok.isEmpty()) throw ImportException(firstError ?: "No images were imported.")
            ok
        }

    private fun inspect(file: File): Triple<Int, Int, String> {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, o)
        val mime = o.outMimeType
        if (mime == null || o.outWidth <= 0 || o.outHeight <= 0) throw ImportException("This file is not a readable image.")
        if (mime !in supported)
            throw ImportException("Unsupported format (${Formatters.formatName(mime)}). PostGrab supports JPEG, PNG and WebP.")
        return Triple(o.outWidth, o.outHeight, mime)
    }

    private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun sanitize(s: String) = s.replace(Regex("[^A-Za-z0-9._-]"), "_").take(60).trim('_', '.')

    private fun nameFromUrl(url: String, mime: String): String {
        val last = runCatching { URI(url).path?.substringAfterLast('/') }.getOrNull().orEmpty()
        val stem = sanitize(last.substringBeforeLast('.', last))
        val base = if (stem.isBlank()) "PostGrab_${stamp()}" else stem
        return "$base.${Formatters.extension(mime)}"
    }

    private fun displayName(uri: Uri, mime: String): String {
        var raw: String? = null
        runCatching {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) raw = it.getString(0)
            }
        }
        val stem = sanitize((raw ?: "").substringBeforeLast('.', raw ?: ""))
        val base = if (stem.isBlank()) "PostGrab_${stamp()}" else stem
        return "$base.${Formatters.extension(mime)}"
    }

    private suspend fun recordImport(src: String, type: SourceType, name: String, size: Long, w: Int, h: Int, mime: String): Long {
        val now = System.currentTimeMillis()
        return dao.insert(HistoryEntity(
            source = src, sourceType = type.name, name = name, sizeBytes = size, width = w, height = h,
            mime = mime, status = DownloadStatus.IMPORTED.name, savedUri = null, error = null,
            createdAt = now, updatedAt = now
        ))
    }

    private suspend fun recordFailure(src: String, type: SourceType, error: String) {
        val now = System.currentTimeMillis()
        dao.insert(HistoryEntity(
            source = src, sourceType = type.name, name = src.substringAfterLast('/').take(60).ifBlank { "Image" },
            sizeBytes = 0, width = 0, height = 0, mime = "", status = DownloadStatus.FAILED.name,
            savedUri = null, error = error, createdAt = now, updatedAt = now
        ))
    }

    // ---------- Save ----------

    private class Prepared(val file: File, val mime: String, val name: String)

    override suspend fun save(image: ImportedImage, allowDuplicate: Boolean, onProgress: (Float) -> Unit): SaveResult =
        withContext(Dispatchers.IO) {
            var prepared: Prepared? = null
            try {
                val s = settings.settings.first()
                val p = prepare(image, s)
                prepared = p
                if (!allowDuplicate && gallery.findDuplicate(p.name, p.file.length()))
                    return@withContext SaveResult.Duplicate(p.name)
                val uri = gallery.insert(p.file, p.name, p.mime, onProgress)
                dao.updateStatus(image.historyId, DownloadStatus.DOWNLOADED.name, uri.toString(), null, System.currentTimeMillis())
                Notifier.downloadComplete(ctx, p.name, uri, p.mime)
                SaveResult.Saved(uri, p.name, p.mime)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dao.updateStatus(image.historyId, DownloadStatus.FAILED.name, null, ErrorMessages.of(e), System.currentTimeMillis())
                throw e
            } catch (e: OutOfMemoryError) {
                val ex = ImportException("This image is too large to convert. Choose 'Original' in Settings.")
                dao.updateStatus(image.historyId, DownloadStatus.FAILED.name, null, ex.message, System.currentTimeMillis())
                throw ex
            } finally {
                val p = prepared
                if (p != null && p.file != image.file) p.file.delete()
            }
        }

    @Suppress("DEPRECATION")
    private fun prepare(image: ImportedImage, s: AppSettings): Prepared {
        if (s.format == OutputFormat.ORIGINAL) return Prepared(image.file, image.mime, image.name)
        val (fmt, mime) = when (s.format) {
            OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG to "image/jpeg"
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG to "image/png"
            else -> (if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP) to "image/webp"
        }
        var bmp = BitmapFactory.decodeFile(image.file.path) ?: throw ImportException("Could not decode the image for conversion.")
        if (mime == "image/jpeg" && bmp.hasAlpha()) {
            val flat = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            Canvas(flat).apply { drawColor(Color.WHITE); drawBitmap(bmp, 0f, 0f, null) }
            bmp.recycle()
            bmp = flat
        }
        val out = fetcher.newTempFile()
        try {
            out.outputStream().use { bmp.compress(fmt, s.quality, it) }
        } catch (e: Throwable) { out.delete(); throw e } finally { bmp.recycle() }
        val stem = image.name.substringBeforeLast('.')
        return Prepared(out, mime, "$stem.${Formatters.extension(mime)}")
    }

    // ---------- History / saved images ----------

    override suspend fun deleteHistory(id: Long) = dao.delete(id)
    override suspend fun clearHistory() = dao.clear()

    override fun listSaved(): List<SavedImage> = gallery.list()

    override suspend fun deleteSaved(uris: List<Uri>): List<Uri> = withContext(Dispatchers.IO) {
        val failed = mutableListOf<Uri>()
        uris.forEach {
            try { if (gallery.delete(it) == 0) failed += it } catch (e: SecurityException) { failed += it }
        }
        failed
    }

    private fun HistoryEntity.toItem() = HistoryItem(
        id = id, source = source,
        sourceType = runCatching { SourceType.valueOf(sourceType) }.getOrDefault(SourceType.URL),
        name = name, sizeBytes = sizeBytes, width = width, height = height, mime = mime,
        status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED),
        savedUri = savedUri, error = error, createdAt = createdAt
    )
}
