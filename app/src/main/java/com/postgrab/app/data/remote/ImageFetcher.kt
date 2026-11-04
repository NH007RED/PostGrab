package com.postgrab.app.data.remote

import com.postgrab.app.domain.model.ImportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Downloads an authorized, already-validated https image URL into the app cache. No scraping, no scripts. */
class ImageFetcher(private val importsDir: File) {
    companion object { const val MAX_BYTES = 40L * 1024 * 1024 }

    fun newTempFile(): File {
        importsDir.mkdirs()
        return File(importsDir, "imp_${System.nanoTime()}")
    }

    suspend fun fetch(url: String, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "PostGrab/1.0 (Android)")
        conn.setRequestProperty("Accept", "image/jpeg,image/png,image/webp,image/*;q=0.8")
        val out = newTempFile()
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw ImportException("The server responded with HTTP $code. The image may need permission to access.")
            val type = conn.contentType?.substringBefore(';')?.trim()?.lowercase()
            if (type == null || !type.startsWith("image/"))
                throw ImportException("This link is not a direct image file. Please use an authorized image file or direct image URL.")
            val total = conn.contentLengthLong
            if (total > MAX_BYTES) throw ImportException("This image is too large (over 40 MB).")
            var done = 0L
            conn.inputStream.use { input ->
                out.outputStream().use { o ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        o.write(buf, 0, n)
                        done += n
                        if (done > MAX_BYTES) throw ImportException("This image is too large (over 40 MB).")
                        onProgress(if (total > 0) done.toFloat() / total else -1f)
                    }
                }
            }
            out
        } catch (e: Throwable) {
            out.delete()
            throw e
        } finally {
            conn.disconnect()
        }
    }
}
