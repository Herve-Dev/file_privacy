package com.hervedev.fileprivacy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hervedev.fileprivacy.domain.ClipboardMode
import com.hervedev.fileprivacy.domain.FileClipboard
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.ui.dialogs.DeleteConfirmationDialog
import com.hervedev.fileprivacy.ui.dialogs.FileDetailsDialog
import com.hervedev.fileprivacy.ui.dialogs.RenameDialog
import com.hervedev.fileprivacy.ui.theme.Radius
import com.hervedev.fileprivacy.ui.theme.Spacing
import com.hervedev.fileprivacy.ui.viewmodel.RecentsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    navController: NavController,
    viewModel: RecentsViewModel = viewModel()
) {
    val recentFiles by viewModel.recentFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRecentsEnabled by viewModel.isRecentsEnabled.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var needsBackgroundRefresh by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                needsBackgroundRefresh = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (needsBackgroundRefresh) {
                    viewModel.invalidateCache()
                    viewModel.scanRecents(force = true)
                    needsBackgroundRefresh = false
                } else {
                    viewModel.scanRecents(force = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemToPermanentlyDelete by remember { mutableStateOf<FileItem?>(null) }
    var itemForDetails by remember { mutableStateOf<FileItem?>(null) }
    var menuExpandedItemPath by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Fichiers récents",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.scanRecents(force = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!isRecentsEnabled) {
                    Text(
                        text = "Désactivé dans les réglages",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (isLoading && recentFiles.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (recentFiles.isEmpty()) {
                    Text(
                        text = "Aucun fichier récent",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shadowElevation = 0.dp
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(all = Spacing.small),
                            verticalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            items(recentFiles, key = { it.path }) { item ->
                                Box {
                                    FileListItem(
                                        item = item,
                                        isSelected = false,
                                        isSelectionMode = false,
                                        onClick = { itemForDetails = item },
                                        onLongClick = { menuExpandedItemPath = item.path },
                                        onInfoClick = { itemForDetails = item }
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
                                                itemToRename = item
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Infos") },
                                            leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                                            onClick = {
                                                menuExpandedItemPath = null
                                                itemForDetails = item
                                            }
                                        )
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
                                                viewModel.deleteFile(item) { _, message ->
                                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                                }
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
                                                itemToPermanentlyDelete = item
                                            }
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Copier") },
                                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                                            onClick = {
                                                menuExpandedItemPath = null
                                                FileClipboard.set(listOf(item), ClipboardMode.COPY)
                                                scope.launch { snackbarHostState.showSnackbar("'${item.name}' copié") }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Couper") },
                                            leadingIcon = { Icon(Icons.Outlined.ContentCut, contentDescription = null) },
                                            onClick = {
                                                menuExpandedItemPath = null
                                                FileClipboard.set(listOf(item), ClipboardMode.CUT)
                                                scope.launch { snackbarHostState.showSnackbar("'${item.name}' coupé") }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    itemToRename?.let { item ->
        RenameDialog(
            currentName = item.name,
            onDismiss = { itemToRename = null },
            onConfirm = { newName ->
                itemToRename = null
                viewModel.renameFile(item, newName) { _, message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        )
    }

    itemToPermanentlyDelete?.let { item ->
        DeleteConfirmationDialog(
            itemName = item.name,
            title = "Supprimer définitivement ?",
            message = "Voulez-vous vraiment supprimer définitivement \"${item.name}\" ? Cette action est irréversible.",
            confirmButtonText = "Supprimer",
            onDismiss = { itemToPermanentlyDelete = null },
            onConfirm = {
                itemToPermanentlyDelete = null
                viewModel.permanentlyDeleteFile(item) { _, message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        )
    }

    itemForDetails?.let { item ->
        FileDetailsDialog(
            item = item,
            onDismiss = { itemForDetails = null }
        )
    }
}
