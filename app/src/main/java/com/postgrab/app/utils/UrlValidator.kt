package com.postgrab.app.utils

import java.net.URI

sealed class UrlCheck {
    data class Ok(val url: String) : UrlCheck()
    data class Invalid(val message: String) : UrlCheck()
}

object UrlValidator {
    const val YOUTUBE_MESSAGE =
        "Unable to retrieve this post image automatically. Please use an authorized image file or direct image URL."

    private val privateIp = Regex("^(10\\.|127\\.|192\\.168\\.|169\\.254\\.|172\\.(1[6-9]|2\\d|3[01])\\.|0\\.).*")
    private val youtubeHosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be", "youtube-nocookie.com")

    fun check(raw: String): UrlCheck {
        val text = raw.trim()
        if (text.isEmpty()) return UrlCheck.Invalid("Paste an image link first.")
        if (text.any { it.isWhitespace() }) return UrlCheck.Invalid("This link contains spaces. Please check it.")
        val uri = runCatching { URI(text) }.getOrNull()
            ?: return UrlCheck.Invalid("This does not look like a valid link.")
        val scheme = uri.scheme?.lowercase()
        if (scheme == "http") return UrlCheck.Invalid("Only secure https:// links are supported.")
        if (scheme != "https") return UrlCheck.Invalid("Please enter a link starting with https://")
        val host = uri.host?.lowercase() ?: return UrlCheck.Invalid("This link has no valid website address.")
        if (host == "localhost" || host.endsWith(".local") || host.endsWith(".internal") || privateIp.matches(host))
            return UrlCheck.Invalid("Links to private or local addresses are not allowed.")
        // No authorized way to read YouTube posts: never scrape or call private endpoints.
        if (host in youtubeHosts) return UrlCheck.Invalid(YOUTUBE_MESSAGE)
        return UrlCheck.Ok(text)
    }
}
