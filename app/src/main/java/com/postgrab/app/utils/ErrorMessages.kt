package com.postgrab.app.utils

import com.postgrab.app.domain.model.ImportException
import com.postgrab.app.domain.model.InsufficientStorageException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object ErrorMessages {
    fun of(e: Throwable): String = when (e) {
        is ImportException -> e.message ?: "Could not import this image."
        is InsufficientStorageException -> e.message ?: "Not enough storage space."
        is UnknownHostException -> "No internet connection, or the website could not be found."
        is SocketTimeoutException -> "The connection timed out. Please try again."
        is ConnectException -> "Could not connect to the server. Check your internet connection."
        is SSLException -> "A secure connection could not be established."
        is SecurityException -> "Permission denied for this file."
        is OutOfMemoryError -> "This image is too large to process on this device."
        is IOException ->
            if (e.message?.contains("ENOSPC") == true || e.message?.contains("No space", true) == true)
                "Not enough storage space to save this image."
            else "A network or file error occurred: ${e.message ?: "unknown"}"
        else -> "Something went wrong: ${e.message ?: e.javaClass.simpleName}"
    }
}
