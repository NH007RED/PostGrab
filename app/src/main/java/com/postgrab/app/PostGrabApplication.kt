package com.postgrab.app

import android.app.Application
import android.content.Context
import com.postgrab.app.data.local.AppDatabase
import com.postgrab.app.data.local.GalleryStore
import com.postgrab.app.data.local.SettingsStore
import com.postgrab.app.data.remote.ImageFetcher
import com.postgrab.app.data.repository.ImageRepositoryImpl
import com.postgrab.app.domain.repository.ImageRepository
import com.postgrab.app.utils.Notifier
import java.io.File

class AppContainer(context: Context) {
    private val db = AppDatabase.create(context)
    val settingsStore = SettingsStore(context)
    val repository: ImageRepository = ImageRepositoryImpl(
        ctx = context,
        dao = db.historyDao(),
        fetcher = ImageFetcher(File(context.cacheDir, "imports")),
        gallery = GalleryStore(context),
        settings = settingsStore
    )
}

class PostGrabApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        // Clear leftover temporary imports from previous sessions.
        File(cacheDir, "imports").listFiles()?.forEach { it.delete() }
    }
}
