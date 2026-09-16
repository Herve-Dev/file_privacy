package com.hervedev.fileprivacy.ui

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.hervedev.fileprivacy.data.ThumbnailExtractors
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.isApk
import com.hervedev.fileprivacy.domain.isAudio
import com.hervedev.fileprivacy.domain.isImage
import com.hervedev.fileprivacy.domain.isPdf
import com.hervedev.fileprivacy.domain.isVideo
import com.hervedev.fileprivacy.ui.components.AppCard
import com.hervedev.fileprivacy.ui.components.DuotoneFolderIcon
import com.hervedev.fileprivacy.ui.theme.Radius
import com.hervedev.fileprivacy.ui.theme.Spacing
import com.hervedev.fileprivacy.ui.theme.getTypeColor
import java.io.File

@Composable
fun FileGridView(
    fileItems: List<FileItem>,
    selectedPaths: Set<String>,
    isSelectionMode: Boolean,
    sourceType: String,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onInfoClick: (FileItem) -> Unit,
    onRenameClick: (FileItem) -> Unit,
    onDeleteClick: (FileItem) -> Unit,
    onPermanentlyDeleteClick: (FileItem) -> Unit,
    onCopyClick: (FileItem) -> Unit,
    onCutClick: (FileItem) -> Unit,
    onToggleSelection: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
    onFetchFolderThumbnail: (suspend (String) -> String?)? = null
) {
    var menuExpandedItemPath by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 0.dp
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(all = Spacing.small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            items(fileItems, key = { it.path }) { item ->
                val isSelected = selectedPaths.contains(item.path)

                Box {
                    FileGridItem(
                        item = item,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        sourceType = sourceType,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) },
                        onFetchFolderThumbnail = onFetchFolderThumbnail
                    )

                    DropdownMenu(
                        expanded = (menuExpandedItemPath == item.path),
                        onDismissRequest = { menuExpandedItemPath = null },
                        shape = RoundedCornerShape(Radius.card)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Renommer") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                menuExpandedItemPath = null
                                onRenameClick(item)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Infos") },
                            leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                            onClick = {
                                menuExpandedItemPath = null
                                onInfoClick(item)
                            }
                        )
                        if (sourceType == "local") {
                            DropdownMenuItem(
                                text = { Text("Mettre à la corbeille", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpandedItemPath = null
                                    onDeleteClick(item)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Supprimer définitivement", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.DeleteForever,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpandedItemPath = null
                                    onPermanentlyDeleteClick(item)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpandedItemPath = null
                                    onDeleteClick(item)
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Copier") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpandedItemPath = null
                                onCopyClick(item)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Couper") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCut, contentDescription = null) },
                            onClick = {
                                menuExpandedItemPath = null
                                onCutClick(item)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sélectionner") },
                            leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
                            onClick = {
                                menuExpandedItemPath = null
                                onToggleSelection(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridItem(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    sourceType: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFetchFolderThumbnail: (suspend (String) -> String?)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLocalOrExternal = (sourceType == "local" || sourceType == "external")

    val isImageFile = item.isImage() && isLocalOrExternal
    val isVideoFile = item.isVideo() && isLocalOrExternal
    val isAudioFile = item.isAudio() && isLocalOrExternal
    val isPdfFile = item.isPdf() && isLocalOrExternal
    val isApkFile = item.isApk() && isLocalOrExternal

    var folderThumbnailPath by remember(item.path) { mutableStateOf<String?>(null) }
    var extractedBitmap by remember(item.path) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.path) {
        if (isLocalOrExternal) {
            if (item.isDirectory && onFetchFolderThumbnail != null) {
                folderThumbnailPath = onFetchFolderThumbnail(item.path)
            } else if (isAudioFile) {
                extractedBitmap = ThumbnailExtractors.extractAudioThumbnail(item.path)
            } else if (isPdfFile) {
                extractedBitmap = ThumbnailExtractors.extractPdfThumbnail(item.path)
            } else if (isApkFile) {
                extractedBitmap = ThumbnailExtractors.extractApkIcon(context, item.path)
            }
        }
    }

    val hasVisualThumbnail = isImageFile || isVideoFile || folderThumbnailPath != null || extractedBitmap != null

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(Radius.item))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(Radius.item),
        containerColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasVisualThumbnail) {
                val imageModel: Any = when {
                    extractedBitmap != null -> extractedBitmap!!
                    isImageFile || isVideoFile -> File(item.path)
                    else -> File(folderThumbnailPath!!)
                }

                val requestBuilder = ImageRequest.Builder(LocalContext.current)
                    .data(imageModel)
                    .crossfade(true)

                if (isVideoFile) {
                    requestBuilder.videoFrameMillis(1000)
                }

                AsyncImage(
                    model = requestBuilder.build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (isVideoFile) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = "Vidéo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (item.isDirectory) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = "Dossier",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (item.isDirectory) {
                        DuotoneFolderIcon(size = 42.dp)
                    } else {
                        val iconVector = when {
                            isAudioFile -> Icons.Outlined.AudioFile
                            isPdfFile -> Icons.Outlined.PictureAsPdf
                            isApkFile -> Icons.Outlined.Android
                            else -> Icons.Outlined.Description
                        }
                        Icon(
                            imageVector = iconVector,
                            contentDescription = item.name,
                            tint = item.getTypeColor(),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.small))

                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(2.dp)
                )
            }
        }
    }
}
