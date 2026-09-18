package com.hervedev.fileprivacy.ui

import android.os.Environment
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavController
import com.hervedev.fileprivacy.data.FileCategory
import com.hervedev.fileprivacy.data.StorageVolumesHelper
import com.hervedev.fileprivacy.domain.ClipboardMode
import com.hervedev.fileprivacy.domain.FileClipboard
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.domain.ImageViewerSession
import com.hervedev.fileprivacy.domain.isImage
import com.hervedev.fileprivacy.ui.components.AppCard
import com.hervedev.fileprivacy.ui.components.CategoryItemCard
import com.hervedev.fileprivacy.ui.components.FileSearchBar
import com.hervedev.fileprivacy.ui.components.SearchFilterSheet
import com.hervedev.fileprivacy.ui.components.StorageOverviewCard
import com.hervedev.fileprivacy.ui.dialogs.DeleteConfirmationDialog
import com.hervedev.fileprivacy.ui.dialogs.FileDetailsDialog
import com.hervedev.fileprivacy.ui.dialogs.RenameDialog
import com.hervedev.fileprivacy.ui.navigation.NavRoutes
import com.hervedev.fileprivacy.ui.theme.FileTypeBadges
import com.hervedev.fileprivacy.ui.theme.Radius
import com.hervedev.fileprivacy.ui.theme.Spacing
import com.hervedev.fileprivacy.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val trashCount by viewModel.trashCount.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val isCategoriesEnabled by viewModel.isCategoriesEnabled.collectAsState()
    val isRecentsEnabled by viewModel.isRecentsEnabled.collectAsState()
    val isScanningCategories by viewModel.isScanningCategories.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchTypeFilter by viewModel.searchTypeFilter.collectAsState()
    val searchDateFilter by viewModel.searchDateFilter.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var needsBackgroundRefresh by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemForDetails by remember { mutableStateOf<FileItem?>(null) }
    var menuExpandedItemPath by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                needsBackgroundRefresh = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshVolumes()
                if (needsBackgroundRefresh) {
                    viewModel.invalidateCache()
                    viewModel.scanCategories(force = true)
                    needsBackgroundRefresh = false
                } else {
                    viewModel.scanCategories(force = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val internalRootPath = remember { Environment.getExternalStorageDirectory().absolutePath }
    val internalSpaceInfo = remember(internalRootPath) {
        StorageVolumesHelper.getStorageSpaceInfo(internalRootPath)
    }

    val usedGb = internalSpaceInfo?.let { it.usedBytes / (1024f * 1024f * 1024f) } ?: 0f
    val totalGb = internalSpaceInfo?.let { it.totalBytes / (1024f * 1024f * 1024f) } ?: 0f

    fun getCountText(category: FileCategory): String {
        return when {
            !isCategoriesEnabled -> "Désactivé"
            isScanningCategories -> "..."
            else -> "${categoryCounts[category] ?: 0}"
        }
    }

    val isSearchActive = searchQuery.trim().length >= 2

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FilePrivacy",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate(NavRoutes.TRASH) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Corbeille",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isScanningCategories,
            onRefresh = { viewModel.scanCategories(force = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                // 1. Barre de recherche
                item {
                    FileSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.searchQuery.value = it },
                        onFilterClick = { showFilterSheet = true },
                        onClearClick = { viewModel.clearSearch() },
                        hasActiveFilter = (searchTypeFilter != null || searchDateFilter != null)
                    )
                }

                if (isSearchActive) {
                    // Mode Recherche actif : Résultats de recherche
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Résultats (${searchResults.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Effacer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { viewModel.clearSearch() }
                            )
                        }
                    }

                    if (isSearching) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.large),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (searchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.large),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aucun résultat trouvé",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shadowElevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.small)
                                ) {
                                    searchResults.forEachIndexed { index, fileItem ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                thickness = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        }
                                        Box {
                                            FileListItem(
                                                item = fileItem,
                                                isSelected = false,
                                                isSelectionMode = false,
                                                onClick = {
                                                    if (fileItem.isImage()) {
                                                        val images = searchResults.filter { it.isImage() }
                                                        val idx = images.indexOfFirst { it.path == fileItem.path }
                                                        if (idx >= 0) {
                                                            ImageViewerSession.start(images, idx, "local")
                                                            navController.navigate(NavRoutes.IMAGE_VIEWER)
                                                        }
                                                    } else {
                                                        itemForDetails = fileItem
                                                    }
                                                },
                                                onLongClick = { menuExpandedItemPath = fileItem.path },
                                                onInfoClick = { itemForDetails = fileItem }
                                            )

                                            DropdownMenu(
                                                expanded = (menuExpandedItemPath == fileItem.path),
                                                onDismissRequest = { menuExpandedItemPath = null },
                                                shape = RoundedCornerShape(Radius.card)
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Renommer") },
                                                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                                    onClick = {
                                                        menuExpandedItemPath = null
                                                        itemToRename = fileItem
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Infos") },
                                                    leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                                                    onClick = {
                                                        menuExpandedItemPath = null
                                                        itemForDetails = fileItem
                                                    }
                                                )
                                                HorizontalDivider()
                                                DropdownMenuItem(
                                                    text = { Text("Copier") },
                                                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                                                    onClick = {
                                                        menuExpandedItemPath = null
                                                        FileClipboard.set(listOf(fileItem), ClipboardMode.COPY)
                                                        scope.launch { snackbarHostState.showSnackbar("'${fileItem.name}' copié") }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Couper") },
                                                    leadingIcon = { Icon(Icons.Outlined.ContentCut, contentDescription = null) },
                                                    onClick = {
                                                        menuExpandedItemPath = null
                                                        FileClipboard.set(listOf(fileItem), ClipboardMode.CUT)
                                                        scope.launch { snackbarHostState.showSnackbar("'${fileItem.name}' coupé") }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Mode Normal : Dashboard
                    // 2. Carte "Stockage appareil" résumée
                    item {
                        StorageOverviewCard(
                            usedGb = usedGb,
                            totalGb = totalGb,
                            onSeeAllClick = {
                                navController.navigate(NavRoutes.STORAGE) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.clickable {
                                navController.navigate(NavRoutes.fileListRoute("local", internalRootPath))
                            }
                        )
                    }

                    // 3. Grille 3 colonnes x 2 lignes pour les 6 catégories
                    item {
                        Text(
                            text = "Catégories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = Spacing.extraSmall)
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CategoryItemCard(
                                        title = "Images",
                                        count = getCountText(FileCategory.IMAGES),
                                        icon = Icons.Outlined.Image,
                                        accentColor = FileTypeBadges.ImageAccent,
                                        bgColor = FileTypeBadges.ImageBg,
                                        onClick = {
                                            if (isCategoriesEnabled) {
                                                navController.navigate(NavRoutes.categoryResultRoute(FileCategory.IMAGES.name))
                                            }
                                        }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    CategoryItemCard(
                                        title = "Vidéos",
                                        count = getCountText(FileCategory.VIDEOS),
                                        icon = Icons.Outlined.Movie,
                                        accentColor = FileTypeBadges.VideoAccent,
                                        bgColor = FileTypeBadges.VideoBg,
                                        onClick = {
                                            if (isCategoriesEnabled) {
                                                navController.navigate(NavRoutes.categoryResultRoute(FileCategory.VIDEOS.name))
                                            }
                                        }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    CategoryItemCard(
                                        title = "Audio",
                                        count = getCountText(FileCategory.AUDIO),
                                        icon = Icons.Outlined.AudioFile,
                                        accentColor = FileTypeBadges.AudioAccent,
                                        bgColor = FileTypeBadges.AudioBg,
                                        onClick = {
                                            if (isCategoriesEnabled) {
                                                navController.navigate(NavRoutes.categoryResultRoute(FileCategory.AUDIO.name))
                                            }
                                        }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CategoryItemCard(
                                        title = "Documents",
                                        count = getCountText(FileCategory.DOCUMENTS),
                                        icon = Icons.Outlined.Description,
                                        accentColor = FileTypeBadges.DocumentAccent,
                                        bgColor = FileTypeBadges.DocumentBg,
                                        onClick = {
                                            if (isCategoriesEnabled) {
                                                navController.navigate(NavRoutes.categoryResultRoute(FileCategory.DOCUMENTS.name))
                                            }
                                        }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    CategoryItemCard(
                                        title = "Téléchargements",
                                        count = getCountText(FileCategory.DOWNLOADS),
                                        icon = Icons.Outlined.Download,
                                        accentColor = FileTypeBadges.FolderAccent,
                                        bgColor = FileTypeBadges.FolderBg,
                                        onClick = {
                                            if (isCategoriesEnabled) {
                                                navController.navigate(NavRoutes.categoryResultRoute(FileCategory.DOWNLOADS.name))
                                            }
                                        }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    CategoryItemCard(
                                        title = "APK",
                                        count = getCountText(FileCategory.APK),
                                        icon = Icons.Outlined.Android,
                                        accentColor = FileTypeBadges.ApkAccent,
                                        bgColor = FileTypeBadges.ApkBg,
                                        onClick = {
                                            if (isCategoriesEnabled) {
                                                navController.navigate(NavRoutes.categoryResultRoute(FileCategory.APK.name))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 4. Section "Fichiers récents" (aperçu réels)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fichiers récents",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Voir tout",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    navController.navigate(NavRoutes.RECENTS) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }

                    item {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Radius.card)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.small)
                            ) {
                                if (!isRecentsEnabled) {
                                    Text(
                                        text = "Désactivé dans les réglages",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(Spacing.medium)
                                    )
                                } else if (recentFiles.isEmpty()) {
                                    Text(
                                        text = "Aucun fichier récent",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(Spacing.medium)
                                    )
                                } else {
                                    recentFiles.forEachIndexed { index, fileItem ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                thickness = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        }
                                        FileListItem(
                                            item = fileItem,
                                            isSelected = false,
                                            isSelectionMode = false,
                                            onClick = {
                                                if (fileItem.isImage()) {
                                                    val images = recentFiles.filter { it.isImage() }
                                                    val idx = images.indexOfFirst { it.path == fileItem.path }
                                                    if (idx >= 0) {
                                                        ImageViewerSession.start(images, idx, "local")
                                                        navController.navigate(NavRoutes.IMAGE_VIEWER)
                                                    }
                                                } else {
                                                    itemForDetails = fileItem
                                                }
                                            },
                                            onLongClick = {},
                                            onInfoClick = {}
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Raccourci Corbeille
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Utilitaire",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    item {
                        AppCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(NavRoutes.TRASH) },
                            shape = RoundedCornerShape(Radius.card)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.medium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Corbeille",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(end = 4.dp)
                                )

                                Spacer(modifier = Modifier.width(Spacing.medium))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Corbeille",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (trashCount > 0) "$trashCount élément(s)" else "Vide",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Sheet
    if (showFilterSheet) {
        SearchFilterSheet(
            selectedType = searchTypeFilter,
            selectedDate = searchDateFilter,
            onTypeSelect = { viewModel.searchTypeFilter.value = it },
            onDateSelect = { viewModel.searchDateFilter.value = it },
            onResetFilters = {
                viewModel.searchTypeFilter.value = null
                viewModel.searchDateFilter.value = null
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Dialogs
    itemToRename?.let { item ->
        RenameDialog(
            currentName = item.name,
            onDismiss = { itemToRename = null },
            onConfirm = { newName ->
                itemToRename = null
                // Refresh search
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
