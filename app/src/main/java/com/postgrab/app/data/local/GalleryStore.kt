package com.postgrab.app.data.local

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.postgrab.app.domain.model.InsufficientStorageException
import com.postgrab.app.domain.model.SavedImage
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.io.OutputStream

/** All MediaStore work: saves into Pictures/PostGrab, lists and deletes the app's saved images. */
class GalleryStore(private val ctx: Context) {
    private val resolver get() = ctx.contentResolver

    private val collection: Uri
        get() = if (Build.VERSION.SDK_INT >= 29)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    @Suppress("DEPRECATION")
    private fun folderSelection(): Pair<String, Array<String>> =
        if (Build.VERSION.SDK_INT >= 29)
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?" to arrayOf("%Pictures/PostGrab%")
        else
            "${MediaStore.Images.Media.DATA} LIKE ?" to arrayOf("%/Pictures/PostGrab/%")

    fun list(): List<SavedImage> {
        val proj = arrayOf(
            MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE
        )
        val (sel, args) = folderSelection()
        val out = mutableListOf<SavedImage>()
        resolver.query(collection, proj, sel, args, "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { c ->
            val idI = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameI = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeI = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateI = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val mimeI = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            while (c.moveToNext()) {
                out += SavedImage(
                    uri = ContentUris.withAppendedId(collection, c.getLong(idI)),
                    name = c.getString(nameI) ?: "image",
                    sizeBytes = c.getLong(sizeI),
                    dateMs = c.getLong(dateI) * 1000L,
                    mime = c.getString(mimeI) ?: "image/*"
                )
            }
        }
        return out
    }

    fun findDuplicate(name: String, size: Long): Boolean {
        val (sel, args) = folderSelection()
        val selection = "$sel AND ${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.SIZE} = ?"
        val a = args + arrayOf(name, size.toString())
        resolver.query(collection, arrayOf(MediaStore.Images.Media._ID), selection, a, null)?.use {
            return it.count > 0
        }
        return false
    }

    suspend fun insert(src: File, displayName: String, mime: String, onProgress: (Float) -> Unit): Uri {
        val free = StatFs(Environment.getExternalStorageDirectory().path).availableBytes
        if (free < src.length() + 2L * 1024 * 1024) throw InsufficientStorageException()

        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PostGrab")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            // MediaStore never overwrites: a name clash gets a "(1)" suffix automatically.
            val uri = resolver.insert(collection, values) ?: throw IOException("Could not create the file.")
            try {
                val out = resolver.openOutputStream(uri) ?: throw IOException("Could not open the file.")
                out.use { copy(src, it, onProgress) }
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
                return uri
            } catch (e: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                throw e
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PostGrab")
            if (!dir.exists() && !dir.mkdirs()) throw IOException("Could not create the PostGrab folder.")
            val target = uniqueFile(dir, displayName)
            try {
                target.outputStream().use { copy(src, it, onProgress) }
            } catch (e: Throwable) {
                target.delete()
                throw e
            }
            val values = ContentValues().apply {
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA, target.absolutePath)
                put(MediaStore.Images.Media.DISPLAY_NAME, target.name)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.SIZE, target.length())
            }
            return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Could not register the file in the gallery.")
        }
    }

    fun delete(uri: Uri): Int = resolver.delete(uri, null, null)

    private suspend fun copy(src: File, out: OutputStream, onProgress: (Float) -> Unit) {
        val total = src.length().coerceAtLeast(1L)
        var done = 0L
        src.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                done += n
                onProgress(done.toFloat() / total)
            }
        }
        out.flush()
    }

    private fun uniqueFile(dir: File, name: String): File {
        var f = File(dir, name)
        if (!f.exists()) return f
        val stem = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var i = 1
        while (f.exists()) {
            f = File(dir, if (ext.isEmpty()) "$stem ($i)" else "$stem ($i).$ext")
            i++
        }
        return f
    }
}
