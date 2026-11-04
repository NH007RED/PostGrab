package com.postgrab.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    fun fileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var v = bytes.toDouble()
        var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return if (i == 0) "$bytes B" else String.format(Locale.getDefault(), "%.1f %s", v, units[i])
    }

    fun dateTime(ms: Long): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(ms))

    fun date(ms: Long): String =
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ms))

    fun formatName(mime: String): String = when (mime) {
        "image/jpeg" -> "JPEG"
        "image/png" -> "PNG"
        "image/webp" -> "WebP"
        else -> mime.substringAfter('/').uppercase()
    }

    fun extension(mime: String): String = when (mime) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "img"
    }
}
