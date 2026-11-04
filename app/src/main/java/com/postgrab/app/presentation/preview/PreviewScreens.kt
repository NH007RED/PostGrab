package com.postgrab.app.presentation.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.postgrab.app.domain.model.ImportedImage
import com.postgrab.app.presentation.MainUiState
import com.postgrab.app.presentation.SaveState
import com.postgrab.app.presentation.components.EmptyState
import com.postgrab.app.presentation.components.InfoChip
import com.postgrab.app.utils.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit,
    onDownload: () -> Unit,
    onDownloadAll: () -> Unit,
    onCancelSave: () -> Unit,
    onConfirmDuplicate: () -> Unit,
    onDismissDuplicate: () -> Unit,
    onConsumeSave: () -> Unit,
    onFullscreen: (ImportedImage) -> Unit,
    onShare: (ImportedImage) -> Unit
) {
    val images = state.images
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.save) {
        when (val s = state.save) {
            is SaveState.Saved -> { snack.showSnackbar("Saved ${s.name} to Pictures/PostGrab"); onConsumeSave() }
            is SaveState.Failed -> { snack.showSnackbar(s.message); onConsumeSave() }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }
            )
        }
    ) { pad ->
        if (images.isEmpty()) {
            EmptyState(Icons.Rounded.Image, "Nothing to preview", "Import an image to see it here.", Modifier.padding(pad))
            return@Scaffold
        }
        val pager = rememberPagerState(initialPage = state.index.coerceIn(0, images.lastIndex)) { images.size }
        LaunchedEffect(pager.currentPage) { onSelect(pager.currentPage) }
        val current = images[pager.currentPage.coerceIn(0, images.lastIndex)]
        val saving = state.save is SaveState.Saving

        Column(Modifier.padding(pad).padding(horizontal = 20.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), pageSpacing = 12.dp) { page ->
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { onFullscreen(images[page]) }
                ) {
                    AsyncImage(
                        model = images[page].file, contentDescription = images[page].name,
                        contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()
                    )
                    Icon(
                        Icons.Rounded.Fullscreen, "Full screen",
                        Modifier.align(Alignment.TopEnd).padding(12.dp).clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f)).padding(6.dp),
                        tint = Color.White
                    )
                }
            }

            if (images.size > 1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                        enabled = pager.currentPage > 0
                    ) { Icon(Icons.Rounded.ChevronLeft, "Previous image") }
                    Text("${pager.currentPage + 1}/${images.size}", style = MaterialTheme.typography.titleMedium)
                    IconButton(
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        enabled = pager.currentPage < images.lastIndex
                    ) { Icon(Icons.Rounded.ChevronRight, "Next image") }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("Resolution", "${current.width} × ${current.height}", Modifier.weight(1f))
                InfoChip("File size", Formatters.fileSize(current.sizeBytes), Modifier.weight(1f))
                InfoChip("Format", Formatters.formatName(current.mime), Modifier.weight(1f))
            }

            val s = state.save
            if (s is SaveState.Saving) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        TextButton(onClick = onCancelSave) { Text("Cancel") }
                    }
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onDownload, enabled = !saving, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Rounded.Download, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Download")
                }
                FilledTonalButton(onClick = { onShare(current) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Rounded.Share, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Share")
                }
            }
            if (images.size > 1) {
                OutlinedButton(onClick = onDownloadAll, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                    Text("Download all ${images.size}")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    (state.save as? SaveState.Duplicate)?.let { d ->
        AlertDialog(
            onDismissRequest = onDismissDuplicate,
            title = { Text("Already saved") },
            text = { Text("${d.name} already exists in Pictures/PostGrab. Save another copy? Existing files are never overwritten.") },
            confirmButton = { TextButton(onClick = onConfirmDuplicate) { Text("Save a copy") } },
            dismissButton = { TextButton(onClick = onDismissDuplicate) { Text("Skip") } }
        )
    }
}

@Composable
fun ViewerScreen(uri: String, onBack: () -> Unit, onShare: (() -> Unit)?) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = uri, contentDescription = "Full screen image", contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f; offset = Offset.Zero })
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    }
                }
                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
        )
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = Color.White)) {
                Icon(Icons.Rounded.Close, "Close")
            }
            if (onShare != null) IconButton(onClick = onShare, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = Color.White)) {
                Icon(Icons.Rounded.Share, "Share")
            }
        }
    }
}
