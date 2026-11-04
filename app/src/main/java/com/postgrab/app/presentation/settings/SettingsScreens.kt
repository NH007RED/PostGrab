package com.postgrab.app.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.postgrab.app.domain.model.AppSettings
import com.postgrab.app.domain.model.OutputFormat
import com.postgrab.app.domain.model.ThemeMode
import com.postgrab.app.presentation.components.SectionTitle

private const val PRIVACY_TEXT =
    "PostGrab keeps everything on your device.\n\n" +
    "• Imported images and your history are stored locally and are never uploaded.\n" +
    "• The app only contacts the website of an image link you enter, to download that one file.\n" +
    "• PostGrab never asks for YouTube or Google passwords and does not use private APIs, scraping or access-control bypasses.\n" +
    "• Photos are chosen with the Android Photo Picker, so no broad storage permission is needed.\n" +
    "• Only download images you own or have permission to use."

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(title)
            content()
        }
    }
}

@Composable
private fun LinkRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onTheme: (ThemeMode) -> Unit,
    onDynamic: (Boolean) -> Unit,
    onFormat: (OutputFormat) -> Unit,
    onQuality: (Int) -> Unit,
    onAbout: () -> Unit
) {
    val ctx = LocalContext.current
    var showPrivacy by remember { mutableStateOf(false) }
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0.0" }
    var quality by remember(settings.quality) { mutableFloatStateOf(settings.quality.toFloat()) }
    val lossy = settings.format == OutputFormat.JPEG || settings.format == OutputFormat.WEBP

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard("Appearance") {
                val labels = mapOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { i, m ->
                        SegmentedButton(
                            selected = settings.theme == m, onClick = { onTheme(m) },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                            label = { Text(labels.getValue(m)) }
                        )
                    }
                }
                if (Build.VERSION.SDK_INT >= 31) Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Dynamic color", style = MaterialTheme.typography.bodyLarge)
                        Text("Use colors from your wallpaper", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.dynamicColor, onCheckedChange = onDynamic)
                }
            }

            SettingsCard("Image quality") {
                Text("Save as", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutputFormat.entries.forEach { f ->
                        FilterChip(
                            selected = settings.format == f, onClick = { onFormat(f) },
                            label = { Text(when (f) { OutputFormat.ORIGINAL -> "Original"; OutputFormat.JPEG -> "JPEG"; OutputFormat.PNG -> "PNG"; OutputFormat.WEBP -> "WebP" }) }
                        )
                    }
                }
                Text(
                    if (lossy) "Quality: ${quality.toInt()}%" else "Quality applies to JPEG and WebP only",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = quality, onValueChange = { quality = it }, onValueChangeFinished = { onQuality(quality.toInt()) },
                    valueRange = 50f..100f, enabled = lossy
                )
            }

            SettingsCard("Storage") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Download folder", style = MaterialTheme.typography.bodyLarge)
                        Text("Pictures/PostGrab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    "Images are saved with MediaStore, so no storage permission is needed on Android 10+. Existing files are never overwritten.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCard("About") {
                LinkRow(Icons.Rounded.PrivacyTip, "Privacy policy", null) { showPrivacy = true }
                LinkRow(Icons.Rounded.Info, "About PostGrab", "Version $version", onAbout)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showPrivacy) AlertDialog(
        onDismissRequest = { showPrivacy = false },
        title = { Text("Privacy policy") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(PRIVACY_TEXT, style = MaterialTheme.typography.bodyMedium) } },
        confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Close") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0.0" }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.CloudDownload, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimary) }
            Text("PostGrab – Image Downloader", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text("Version $version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "A privacy-first way to import, preview and save images you're authorized to keep. " +
                    "Works with direct image links, the Android Share sheet, the Photo Picker and your own files. " +
                    "Everything stays on your device.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
            )
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Our principles")
                    listOf(
                        "No YouTube passwords or Google credentials are ever requested.",
                        "No scraping, private endpoints or access-control bypasses.",
                        "Only authorized images — you decide what to import.",
                        "Imported content stays private on your device."
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            TextButton(onClick = {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/htmlmovievg")))
            }) { Text("Developed by NH Developer") }
        }
    }
}
