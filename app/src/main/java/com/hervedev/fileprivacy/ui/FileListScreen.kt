package com.hervedev.fileprivacy.ui

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.ui.navigation.NavRoutes
import com.hervedev.fileprivacy.ui.viewmodel.FileListViewModel
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    navController: NavController,
    viewModel: FileListViewModel = viewModel()
) {
    val currentPath = viewModel.currentPath
    val fileItems by viewModel.fileItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val canNavigateBack = navController.previousBackStackEntry != null

    Scaffold(
        topBar = {
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
                }
            )
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
                        FileListItem(
                            item = item,
                            onClick = {
                                if (item.isDirectory) {
                                    navController.navigate(NavRoutes.fileListRoute(item.path))
                                } else {
                                    // TODO: Aperçu du fichier (Phase 6)
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun FileListItem(
    item: FileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
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
            Icon(
                imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = if (item.isDirectory) "Dossier" else "Fichier",
                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

private fun humanReadableByteCountSI(bytes: Long): String {
    if (-1000 < bytes && bytes < 1000) {
        return "$bytes B"
    }
    val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
    var b = bytes
    while (b <= -999_950 || b >= 999_950) {
        b /= 1000
        ci.next()
    }
    return String.format(Locale.getDefault(), "%.1f %cB", b / 1000.0, ci.current())
}
