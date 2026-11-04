package com.postgrab.app.presentation.downloads

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.postgrab.app.domain.model.SavedImage
import com.postgrab.app.presentation.components.EmptyState
import com.postgrab.app.utils.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DownloadsScreen(
    state: DownloadsUiState,
    onRefresh: () -> Unit,
    onToggle: (Uri) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onOpen: (SavedImage) -> Unit,
    onShare: (List<Uri>) -> Unit,
    onDelete: (List<Uri>) -> Unit,
    onGoImport: () -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }
    val selecting = state.selected.isNotEmpty()
    var pendingDelete by remember { mutableStateOf<List<Uri>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selecting) "${state.selected.size} selected" else "Downloads") },
                navigationIcon = {
                    if (selecting) IconButton(onClick = onClearSelection) { Icon(Icons.Rounded.Close, "Cancel selection") }
                },
                actions = {
                    if (selecting) {
                        IconButton(onClick = onSelectAll) { Icon(Icons.Rounded.SelectAll, "Select all") }
                        IconButton(onClick = { onShare(state.selected.toList()) }) { Icon(Icons.Rounded.Share, "Share") }
                        IconButton(onClick = { pendingDelete = state.selected.toList() }) { Icon(Icons.Rounded.Delete, "Delete") }
                    }
                }
            )
        }
    ) { pad ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.items.isEmpty() -> EmptyState(
                Icons.Rounded.PhotoLibrary, "No saved images yet",
                "Images you download appear here, saved in Pictures/PostGrab.", Modifier.padding(pad),
                action = { FilledTonalButton(onClick = onGoImport) { Text("Import an image") } }
            )
            else -> LazyVerticalGrid(
                GridCells.Fixed(2), Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.items, key = { it.uri.toString() }) { item ->
                    val isSel = item.uri in state.selected
                    var menu by remember { mutableStateOf(false) }
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Box(
                            Modifier.combinedClickable(
                                onClick = { if (selecting) onToggle(item.uri) else onOpen(item) },
                                onLongClick = { onToggle(item.uri) }
                            )
                        ) {
                            AsyncImage(
                                model = item.uri, contentDescription = item.name, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            )
                            if (selecting) Icon(
                                if (isSel) Icons.Rounded.CheckCircle else Icons.Rounded.Check,
                                null, Modifier.align(Alignment.TopStart).padding(8.dp),
                                tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${Formatters.fileSize(item.sizeBytes)} • ${Formatters.date(item.dateMs)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More options") }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text("Open") }, onClick = { menu = false; onOpen(item) })
                                    DropdownMenuItem(text = { Text("Share") }, onClick = { menu = false; onShare(listOf(item.uri)) })
                                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; pendingDelete = listOf(item.uri) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { uris ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (uris.size == 1) "Delete image?" else "Delete ${uris.size} images?") },
            text = { Text("This permanently removes ${if (uris.size == 1) "the image" else "the images"} from your device.") },
            confirmButton = { TextButton(onClick = { pendingDelete = null; onDelete(uris) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}
