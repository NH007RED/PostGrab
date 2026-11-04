package com.postgrab.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.postgrab.app.domain.model.AppSettings
import com.postgrab.app.domain.model.OutputFormat
import com.postgrab.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val ctx: Context) {
    private object K {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC = booleanPreferencesKey("dynamic")
        val FORMAT = stringPreferencesKey("format")
        val QUALITY = intPreferencesKey("quality")
    }

    val settings: Flow<AppSettings> = ctx.dataStore.data.map { p ->
        AppSettings(
            theme = p[K.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            dynamicColor = p[K.DYNAMIC] ?: true,
            format = p[K.FORMAT]?.let { runCatching { OutputFormat.valueOf(it) }.getOrNull() } ?: OutputFormat.ORIGINAL,
            quality = (p[K.QUALITY] ?: 90).coerceIn(50, 100)
        )
    }

    suspend fun setTheme(t: ThemeMode) { ctx.dataStore.edit { it[K.THEME] = t.name } }
    suspend fun setDynamic(v: Boolean) { ctx.dataStore.edit { it[K.DYNAMIC] = v } }
    suspend fun setFormat(f: OutputFormat) { ctx.dataStore.edit { it[K.FORMAT] = f.name } }
    suspend fun setQuality(q: Int) { ctx.dataStore.edit { it[K.QUALITY] = q.coerceIn(50, 100) } }
}
