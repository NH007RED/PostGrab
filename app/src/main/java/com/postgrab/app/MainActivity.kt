package com.postgrab.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.postgrab.app.domain.model.AppSettings
import com.postgrab.app.domain.model.ThemeMode
import com.postgrab.app.presentation.MainViewModel
import com.postgrab.app.presentation.VmFactory
import com.postgrab.app.presentation.navigation.PostGrabRoot
import com.postgrab.app.theme.PostGrabTheme

class MainActivity : ComponentActivity() {
    private val container get() = (application as PostGrabApplication).container
    private val factory by lazy { VmFactory(container) }
    private val mainVm: MainViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) mainVm.handleIntent(intent)

        setContent {
            val settings by container.settingsStore.settings.collectAsStateWithLifecycle(AppSettings())
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                )
                onDispose { }
            }
            PostGrabTheme(darkTheme = dark, dynamicColor = settings.dynamicColor) {
                PostGrabRoot(main = mainVm, factory = factory)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainVm.handleIntent(intent)
    }
}
