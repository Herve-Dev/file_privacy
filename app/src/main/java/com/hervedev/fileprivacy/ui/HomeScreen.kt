package com.hervedev.fileprivacy.ui

import android.os.Environment
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hervedev.fileprivacy.data.StorageVolumesHelper
import com.hervedev.fileprivacy.ui.components.CategoryItemCard
import com.hervedev.fileprivacy.ui.components.FileSearchBar
import com.hervedev.fileprivacy.ui.components.StorageOverviewCard
import com.hervedev.fileprivacy.ui.navigation.NavRoutes
import com.hervedev.fileprivacy.ui.theme.FileTypeBadges
import com.hervedev.fileprivacy.ui.theme.Radius
import com.hervedev.fileprivacy.ui.theme.Spacing
import com.hervedev.fileprivacy.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshVolumes()
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // 1. Barre de recherche
            item {
                FileSearchBar(
                    onSearchClick = {},
                    onSortClick = {}
                )
            }

            // 2. Vue d'ensemble du stockage
            item {
                StorageOverviewCard(
                    usedGb = usedGb,
                    totalGb = totalGb,
                    onSeeAllClick = {
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
                    modifier = Modifier.padding(top = Spacing.small, bottom = Spacing.extraSmall)
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
                                count = "—",
                                icon = Icons.Outlined.Image,
                                accentColor = FileTypeBadges.ImageAccent,
                                bgColor = FileTypeBadges.ImageBg,
                                onClick = {
                                    navController.navigate(NavRoutes.fileListRoute("local", internalRootPath))
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CategoryItemCard(
                                title = "Vidéos",
                                count = "—",
                                icon = Icons.Outlined.Movie,
                                accentColor = FileTypeBadges.VideoAccent,
                                bgColor = FileTypeBadges.VideoBg,
                                onClick = {
                                    navController.navigate(NavRoutes.fileListRoute("local", internalRootPath))
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CategoryItemCard(
                                title = "Audio",
                                count = "—",
                                icon = Icons.Outlined.AudioFile,
                                accentColor = FileTypeBadges.AudioAccent,
                                bgColor = FileTypeBadges.AudioBg,
                                onClick = {
                                    navController.navigate(NavRoutes.fileListRoute("local", internalRootPath))
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
                                count = "—",
                                icon = Icons.Outlined.Description,
                                accentColor = FileTypeBadges.DocumentAccent,
                                bgColor = FileTypeBadges.DocumentBg,
                                onClick = {
                                    navController.navigate(NavRoutes.fileListRoute("local", internalRootPath))
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CategoryItemCard(
                                title = "Téléchargements",
                                count = "—",
                                icon = Icons.Outlined.Download,
                                accentColor = FileTypeBadges.FolderAccent,
                                bgColor = FileTypeBadges.FolderBg,
                                onClick = {
                                    navController.navigate(NavRoutes.fileListRoute("local", internalRootPath))
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CategoryItemCard(
                                title = "APK",
                                count = "—",
                                icon = Icons.Outlined.Android,
                                accentColor = FileTypeBadges.ApkAccent,
                                bgColor = FileTypeBadges.ApkBg,
                                onClick = {
                                    navController.navigate(NavRoutes.fileListRoute("local", internalRootPath))
                                }
                            )
                        }
                    }
                }
            }

            // 4. Fichiers récents dans une carte arrondie avec séparateurs de 1.dp
            item {
                Spacer(modifier = Modifier.height(Spacing.small))
                Text(
                    text = "Fichiers récents",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = Spacing.extraSmall)
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.card),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium)
                    ) {
                        Text(
                            text = "Aucun fichier récent",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.small)
                        )
                    }
                }
            }
        }
    }
}
