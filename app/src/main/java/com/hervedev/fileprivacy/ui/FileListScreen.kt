package com.hervedev.fileprivacy.ui

import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.ui.dialogs.CreateFolderDialog
import com.hervedev.fileprivacy.ui.dialogs.DeleteConfirmationDialog
import com.hervedev.fileprivacy.ui.dialogs.FileDetailsDialog
import com.hervedev.fileprivacy.ui.dialogs.RenameDialog
import com.hervedev.fileprivacy.ui.navigation.NavRoutes
import com.hervedev.fileprivacy.ui.theme.Radius
import com.hervedev.fileprivacy.ui.theme.Spacing
import com.hervedev.fileprivacy.ui.utils.humanReadableByteCountSI
import com.hervedev.fileprivacy.ui.viewmodel.FileListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    navController: NavController,
    viewModel: FileListViewModel = viewModel()
) {
    val connectionId = viewModel.connectionId
    val sourceType = viewModel.sourceType
    val currentPath = viewModel.currentPath
    val fileItems by viewModel.fileItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isStorageAccessible by viewModel.isStorageAccessible.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val clipboardState by viewModel.clipboardState.collectAsState()
    val isGridMode by viewModel.isGridMode.collectAsState()

    val isSelectionMode = selectedPaths.isNotEmpty()
    val canNavigateBack = navController.previousBackStackEntry != null

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemToTrash by remember { mutableStateOf<FileItem?>(null) }
    var itemToPermanentlyDelete by remember { mutableStateOf<FileItem?>(null) }
    var itemForDetails by remember { mutableStateOf<FileItem?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    var showBatchTrashDialog by remember { mutableStateOf(false) }
    var showBatchPermanentDeleteDialog by remember { mutableStateOf(false) }

    var menuExpandedItemPath by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedPaths.size} sélectionné(s)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Annuler la sélection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Outlined.SelectAll, contentDescription = "Tout sélectionner")
                        }
                        IconButton(onClick = {
                            viewModel.copySelected()
                            scope.launch { snackbarHostState.showSnackbar("Éléments copiés dans le presse-papier") }
                        }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copier")
                        }
                        IconButton(onClick = {
                            viewModel.cutSelected()
                            scope.launch { snackbarHostState.showSnackbar("Éléments coupés dans le presse-papier") }
                        }) {
                            Icon(Icons.Outlined.ContentCut, contentDescription = "Couper")
                        }
                        IconButton(onClick = { showBatchTrashDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = if (sourceType == "local") "Mettre à la corbeille" else "Supprimer",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        if (sourceType == "local") {
                            IconButton(onClick = { showBatchPermanentDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteForever,
                                    contentDescription = "Supprimer définitivement",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        val rootPath = if (connectionId != null) "" else Environment.getExternalStorageDirectory().absolutePath
                        BreadcrumbBar(
                            currentPath = currentPath,
                            rootPath = rootPath,
                            onItemClick = { targetPath ->
                                val targetRoute = if (connectionId != null) {
                                    NavRoutes.smbListRoute(connectionId, targetPath)
                                } else {
                                    NavRoutes.fileListRoute(sourceType, targetPath)
                                }

                                val popped = navController.popBackStack(targetRoute, inclusive = false)
                                if (!popped) {
                                    if (connectionId != null) {
                                        navController.navigate(targetRoute) {
                                            popUpTo(NavRoutes.smbListRoute(connectionId, "")) { inclusive = false }
                                        }
                                    } else {
                                        navController.navigate(targetRoute) {
                                            popUpTo(NavRoutes.fileListRoute(sourceType, rootPath)) { inclusive = false }
                                        }
                                    }
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        if (canNavigateBack) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Retour"
                                )
                            }
                        }
                    },
                    actions = {
                        if (sourceType != "smb") {
                            IconButton(onClick = { viewModel.toggleViewMode() }) {
                                Icon(
                                    imageVector = if (isGridMode) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                                    contentDescription = if (isGridMode) "Afficher en liste" else "Afficher en grille"
                                )
                            }
                        }
                        if (clipboardState?.items?.isNotEmpty() == true) {
                            IconButton(
                                onClick = {
                                    viewModel.pasteClipboard { _, message ->
                                        scope.launch { snackbarHostState.showSnackbar(message) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
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
                onClick = { showCreateFolderDialog = true },
                shape = RoundedCornerShape(Radius.card),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
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
            } else if (!isStorageAccessible) {
                Text(
                    text = errorMessage ?: "Ce stockage n'est plus accessible",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (fileItems.isEmpty()) {
                Text(
                    text = "Dossier vide",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (isGridMode && (sourceType == "local" || sourceType == "external")) {
                FileGridView(
                    fileItems = fileItems,
                    selectedPaths = selectedPaths,
                    isSelectionMode = isSelectionMode,
                    sourceType = sourceType,
                    onItemClick = { item ->
                        if (isSelectionMode) {
                            viewModel.toggleSelection(item.path)
                        } else if (item.isDirectory) {
                            if (connectionId != null) {
                                navController.navigate(NavRoutes.smbListRoute(connectionId, item.path))
                            } else {
                                navController.navigate(NavRoutes.fileListRoute(sourceType, item.path))
                            }
                        } else {
                            // TODO: Aperçu du fichier (Phase 6)
                        }
                    },
                    onItemLongClick = { item ->
                        if (!isSelectionMode) {
                            menuExpandedItemPath = item.path
                        }
                    },
                    onInfoClick = { item -> itemForDetails = item },
                    onRenameClick = { item -> itemToRename = item },
                    onDeleteClick = { item ->
                        if (sourceType == "local") {
                            viewModel.deleteFile(item) { _, message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        } else {
                            itemToTrash = item
                        }
                    },
                    onPermanentlyDeleteClick = { item -> itemToPermanentlyDelete = item },
                    onCopyClick = { item ->
                        viewModel.copyItem(item)
                        scope.launch { snackbarHostState.showSnackbar("'${item.name}' copié dans le presse-papier") }
                    },
                    onCutClick = { item ->
                        viewModel.cutItem(item)
                        scope.launch { snackbarHostState.showSnackbar("'${item.name}' coupé dans le presse-papier") }
                    },
                    onToggleSelection = { item -> viewModel.toggleSelection(item.path) },
                    onFetchFolderThumbnail = { folderPath -> viewModel.getFolderThumbnail(folderPath) }
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(all = Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
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
                                            if (connectionId != null) {
                                                navController.navigate(NavRoutes.smbListRoute(connectionId, item.path))
                                            } else {
                                                navController.navigate(NavRoutes.fileListRoute(sourceType, item.path))
                                            }
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
                                                itemToTrash = item
                                            }
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Copier") },
                                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                                        onClick = {
                                            menuExpandedItemPath = null
                                            viewModel.copyItem(item)
                                            scope.launch { snackbarHostState.showSnackbar("'${item.name}' copié dans le presse-papier") }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Couper") },
                                        leadingIcon = { Icon(Icons.Outlined.ContentCut, contentDescription = null) },
                                        onClick = {
                                            menuExpandedItemPath = null
                                            viewModel.cutItem(item)
                                            scope.launch { snackbarHostState.showSnackbar("'${item.name}' coupé dans le presse-papier") }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sélectionner") },
                                        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
                                        onClick = {
                                            menuExpandedItemPath = null
                                            viewModel.toggleSelection(item.path)
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

    itemToTrash?.let { item ->
        DeleteConfirmationDialog(
            itemName = item.name,
            title = "Supprimer l'élément ?",
            message = "Voulez-vous vraiment supprimer \"${item.name}\" ?",
            onDismiss = { itemToTrash = null },
            onConfirm = {
                itemToTrash = null
                viewModel.deleteFile(item) { _, message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        )
    }

    itemToPermanentlyDelete?.let { item ->
        DeleteConfirmationDialog(
            itemName = item.name,
            title = "Supprimer définitivement ?",
            message = "Voulez-vous vraiment supprimer définitivement \"${item.name}\" ? Cette action est définitive et irréversible.",
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

    if (showBatchTrashDialog) {
        val count = selectedPaths.size
        val isLocal = sourceType == "local"
        DeleteConfirmationDialog(
            itemName = "$count élément(s)",
            title = if (isLocal) "Mettre à la corbeille ?" else "Supprimer les éléments ?",
            message = if (isLocal) "Voulez-vous déplacer $count élément(s) vers la corbeille ?" else "Voulez-vous vraiment supprimer $count élément(s) ?",
            onDismiss = { showBatchTrashDialog = false },
            onConfirm = {
                showBatchTrashDialog = false
                viewModel.deleteSelected(permanently = false) { _, message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            }
        )
    }

    if (showBatchPermanentDeleteDialog) {
        val count = selectedPaths.size
        DeleteConfirmationDialog(
            itemName = "$count élément(s)",
            title = "Supprimer définitivement ?",
            message = "Voulez-vous supprimer définitivement $count élément(s) ? Cette action est définitive et irréversible.",
            confirmButtonText = "Supprimer",
            onDismiss = { showBatchPermanentDeleteDialog = false },
            onConfirm = {
                showBatchPermanentDeleteDialog = false
                viewModel.deleteSelected(permanently = true) { _, message ->
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

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            existingNames = fileItems.map { it.name }.toSet(),
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { folderName ->
                showCreateFolderDialog = false
                viewModel.createFolder(folderName) { _, message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.item))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(Radius.item),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shadowElevation = if (isSelected) 6.dp else 3.dp,
        tonalElevation = if (isSelected) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = Spacing.small)
                )
            } else {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Description,
                    contentDescription = if (item.isDirectory) "Dossier" else "Fichier",
                    tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(30.dp)
                        .padding(end = 4.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.medium))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                if (!item.isDirectory) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = humanReadableByteCountSI(item.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isSelectionMode) {
                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Informations",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
