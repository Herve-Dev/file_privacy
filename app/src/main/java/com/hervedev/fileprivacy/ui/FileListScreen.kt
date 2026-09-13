package com.hervedev.fileprivacy.ui

import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.ui.dialogs.CreateFolderDialog
import com.hervedev.fileprivacy.ui.dialogs.DeleteConfirmationDialog
import com.hervedev.fileprivacy.ui.dialogs.FileDetailsDialog
import com.hervedev.fileprivacy.ui.dialogs.RenameDialog
import com.hervedev.fileprivacy.ui.navigation.NavRoutes
import com.hervedev.fileprivacy.ui.utils.humanReadableByteCountSI
import com.hervedev.fileprivacy.ui.viewmodel.FileListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    navController: NavController,
    viewModel: FileListViewModel = viewModel()
) {
    val currentPath = viewModel.currentPath
    val fileItems by viewModel.fileItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val clipboardState by viewModel.clipboardState.collectAsState()

    val isSelectionMode = selectedPaths.isNotEmpty()
    val canNavigateBack = navController.previousBackStackEntry != null

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemToDelete by remember { mutableStateOf<FileItem?>(null) }
    var itemForDetails by remember { mutableStateOf<FileItem?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var menuExpandedItemPath by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedPaths.size} sélectionné(s)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Annuler la sélection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Tout sélectionner")
                        }
                        IconButton(onClick = {
                            viewModel.copySelected()
                            scope.launch { snackbarHostState.showSnackbar("Éléments copiés dans le presse-papier") }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copier")
                        }
                        IconButton(onClick = {
                            viewModel.cutSelected()
                            scope.launch { snackbarHostState.showSnackbar("Éléments coupés dans le presse-papier") }
                        }) {
                            Icon(Icons.Default.ContentCut, contentDescription = "Couper")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        BreadcrumbBar(
                            currentPath = currentPath,
                            onItemClick = { targetPath ->
                                val targetRoute = NavRoutes.fileListRoute(targetPath)
                                val popped = navController.popBackStack(targetRoute, inclusive = false)
                                if (!popped) {
                                    val rootPath = Environment.getExternalStorageDirectory().absolutePath
                                    navController.navigate(targetRoute) {
                                        popUpTo(NavRoutes.fileListRoute(rootPath)) { inclusive = false }
                                    }
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        if (canNavigateBack) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Dossier parent"
                                )
                            }
                        }
                    },
                    actions = {
                        if (clipboardState?.items?.isNotEmpty() == true) {
                            IconButton(
                                onClick = {
                                    viewModel.pasteClipboard { _, message ->
                                        if (message != null) {
                                            scope.launch { snackbarHostState.showSnackbar(message) }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Coller"
                                )
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateFolderDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Créer un dossier"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (fileItems.isEmpty()) {
                Text(
                    text = "Dossier vide",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(fileItems, key = { it.path }) { item ->
                        val isSelected = selectedPaths.contains(item.path)

                        Box {
                            FileListItem(
                                item = item,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(item.path)
                                    } else if (item.isDirectory) {
                                        navController.navigate(NavRoutes.fileListRoute(item.path))
                                    } else {
                                        // TODO: Aperçu du fichier (Phase 6)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        menuExpandedItemPath = item.path
                                    }
                                },
                                onInfoClick = {
                                    itemForDetails = item
                                }
                            )

                            DropdownMenu(
                                expanded = (menuExpandedItemPath == item.path),
                                onDismissRequest = { menuExpandedItemPath = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Renommer") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpandedItemPath = null
                                        itemToRename = item
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Supprimer") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    onClick = {
                                        menuExpandedItemPath = null
                                        itemToDelete = item
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Copier") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        menuExpandedItemPath = null
                                        viewModel.copyItem(item)
                                        scope.launch { snackbarHostState.showSnackbar("'${item.name}' copié dans le presse-papier") }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Couper") },
                                    leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
                                    onClick = {
                                        menuExpandedItemPath = null
                                        viewModel.cutItem(item)
                                        scope.launch { snackbarHostState.showSnackbar("'${item.name}' coupé dans le presse-papier") }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sélectionner") },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                    onClick = {
                                        menuExpandedItemPath = null
                                        viewModel.toggleSelection(item.path)
                                    }
                                )
                            }
                        }
                        HorizontalDivider()
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
                    if (message != null) {
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                }
            }
        )
    }

    itemToDelete?.let { item ->
        DeleteConfirmationDialog(
            itemName = item.name,
            onDismiss = { itemToDelete = null },
            onConfirm = {
                itemToDelete = null
                viewModel.deleteFile(item) { _, message ->
                    if (message != null) {
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
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

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            existingNames = fileItems.map { it.name }.toSet(),
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { folderName ->
                showCreateFolderDialog = false
                viewModel.createFolder(folderName) { _, message ->
                    if (message != null) {
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        headlineContent = {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            if (!item.isDirectory) {
                Text(
                    text = humanReadableByteCountSI(item.sizeBytes),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        leadingContent = {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            } else {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = if (item.isDirectory) "Dossier" else "Fichier",
                    tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            if (!isSelectionMode) {
                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Informations",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}
