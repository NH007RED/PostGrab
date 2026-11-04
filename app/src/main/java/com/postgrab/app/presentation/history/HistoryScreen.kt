package com.postgrab.app.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.postgrab.app.domain.model.DownloadStatus
import com.postgrab.app.domain.model.HistoryItem
import com.postgrab.app.presentation.components.EmptyState
import com.postgrab.app.utils.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    items: List<HistoryItem>,
    onRetry: (HistoryItem) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
    onOpenViewer: (String) -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                actions = {
                    if (items.isNotEmpty()) IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Rounded.DeleteSweep, "Clear history")
                    }
                }
            )
        }
    ) { pad ->
        if (items.isEmpty()) {
            EmptyState(Icons.Rounded.History, "No history yet", "Images you import will be listed here.", Modifier.padding(pad))
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.savedUri != null) AsyncImage(
                                    model = item.savedUri, contentDescription = null,
                                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                                ) else Icon(
                                    if (item.status == DownloadStatus.FAILED) Icons.Rounded.BrokenImage else Icons.Rounded.Image,
                                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    Formatters.dateTime(item.createdAt), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                StatusChip(item.status)
                                if (item.status == DownloadStatus.FAILED && item.error != null)
                                    Text(item.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                IconButton(onClick = { onRemove(item.id) }) { Icon(Icons.Rounded.Close, "Remove from history") }
                                if (item.status == DownloadStatus.FAILED) TextButton(onClick = { onRetry(item) }) {
                                    Icon(Icons.Rounded.Refresh, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Retry")
                                } else if (item.savedUri != null) TextButton(onClick = { onOpenViewer(item.savedUri) }) { Text("Open") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Clear history?") },
        text = { Text("This removes the list only. Saved images stay in your gallery.") },
        confirmButton = { TextButton(onClick = { confirmClear = false; onClear() }) { Text("Clear") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
    )
}

@Composable
private fun StatusChip(status: DownloadStatus) {
    val (label, bg, fg) = when (status) {
        DownloadStatus.DOWNLOADED -> Triple("Downloaded", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        DownloadStatus.IMPORTED -> Triple("Imported, not saved", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        DownloadStatus.FAILED -> Triple("Failed", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }
    Text(
        label, style = MaterialTheme.typography.labelLarge, color = fg,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
