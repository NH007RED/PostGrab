package com.postgrab.app.presentation.navigation

import android.net.Uri

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val IMPORT = "import"
    const val PROGRESS = "progress"
    const val PREVIEW = "preview"
    const val DOWNLOADS = "downloads"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val VIEWER = "viewer/{uri}"
    fun viewer(uri: String) = "viewer/${Uri.encode(uri)}"
}
