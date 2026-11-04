package com.postgrab.app.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.postgrab.app.domain.model.HistoryItem
import com.postgrab.app.presentation.ImportState
import com.postgrab.app.presentation.MainUiState
import com.postgrab.app.presentation.components.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(600))
        alpha.animateTo(1f, tween(300))
        delay(500)
        onFinished()
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(112.dp).scale(scale.value).clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.CloudDownload, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onPrimary) }
            Spacer(Modifier.height(24.dp))
            Text("PostGrab", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.alpha(alpha.value))
            Text(
                "Image Downloader", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}

@Composable
fun HomeScreen(
    state: MainUiState,
    recent: List<HistoryItem>,
    onUrlChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onPhoto: () -> Unit,
    onFiles: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenViewer: (String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.CloudDownload, null, tint = MaterialTheme.colorScheme.onPrimary) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("PostGrab", style = MaterialTheme.typography.titleLarge)
                    Text("Image Downloader", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Welcome", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("Save images you're allowed to keep", style = MaterialTheme.typography.headlineMedium)
            }
        }
        item { UrlInputCard(state.url, onUrlChange, onAnalyze) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Recent downloads")
                if (recent.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Text(
                            "Images you save will appear here.",
                            Modifier.padding(20.dp).fillMaxWidth(), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recent, key = { it.id }) { item ->
                            AsyncImage(
                                model = item.savedUri, contentDescription = item.name, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { item.savedUri?.let(onOpenViewer) }
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(
                onClick = onOpenDownloads, shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PhotoLibrary, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Saved images", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Open your Pictures/PostGrab gallery", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Supported import methods")
                MethodRow(Icons.Rounded.Link, "Direct image URL", "Authorized https links to JPEG, PNG or WebP files", onOpenImport)
                MethodRow(Icons.Rounded.Share, "Android Sharesheet", "Share images or links to PostGrab from any app", null)
                MethodRow(Icons.Rounded.AddPhotoAlternate, "Photo Picker", "Choose photos without extra permissions", onPhoto)
                MethodRow(Icons.Rounded.FolderOpen, "Your own files", "Pick image files you own from storage", onFiles)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    url: String,
    onUrlChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onPhoto: () -> Unit,
    onFiles: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Import images", style = MaterialTheme.typography.headlineMedium) }
        item {
            Text(
                "Only use images you own or have permission to download. YouTube posts can't be fetched automatically.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { UrlInputCard(url, onUrlChange, onAnalyze) }
        item { MethodRow(Icons.Rounded.AddPhotoAlternate, "Photo Picker", "Select one or more photos", onPhoto) }
        item { MethodRow(Icons.Rounded.FolderOpen, "Browse files", "Pick image files you own", onFiles) }
        item { MethodRow(Icons.Rounded.Share, "Share to PostGrab", "Use the Share button in any app and choose PostGrab", null) }
    }
}

@Composable
fun ProgressScreen(state: ImportState, onCancel: () -> Unit, onBack: () -> Unit, onReady: () -> Unit) {
    LaunchedEffect(state) {
        when (state) {
            is ImportState.Ready -> onReady()
            is ImportState.Idle -> onBack()
            else -> Unit
        }
    }
    Box(Modifier.fillMaxSize().systemBarsPadding().padding(32.dp), contentAlignment = Alignment.Center) {
        when (state) {
            is ImportState.Error -> EmptyState(
                icon = Icons.Rounded.ErrorOutline, title = "Couldn't import image", message = state.message,
                action = {
                    Button(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text("Go back")
                    }
                }
            )
            is ImportState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.progress == null) CircularProgressIndicator(Modifier.size(96.dp), strokeWidth = 6.dp)
                    else CircularProgressIndicator(progress = { state.progress }, modifier = Modifier.size(96.dp), strokeWidth = 6.dp)
                    if (state.progress != null)
                        Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
                }
                Text(state.label, style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onCancel) { Icon(Icons.Rounded.Close, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Cancel") }
            }
            else -> CircularProgressIndicator()
        }
    }
}
