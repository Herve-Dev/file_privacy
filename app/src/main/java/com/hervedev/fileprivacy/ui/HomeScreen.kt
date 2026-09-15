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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hervedev.fileprivacy.data.StorageSpaceInfo
import com.hervedev.fileprivacy.data.StorageVolumesHelper
import com.hervedev.fileprivacy.domain.SmbConnection
import com.hervedev.fileprivacy.ui.dialogs.DeleteConfirmationDialog
import com.hervedev.fileprivacy.ui.navigation.NavRoutes
import com.hervedev.fileprivacy.ui.theme.Radius
import com.hervedev.fileprivacy.ui.theme.Spacing
import com.hervedev.fileprivacy.ui.utils.humanReadableByteCountSI
import com.hervedev.fileprivacy.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val externalVolumes by viewModel.externalVolumes.collectAsState()
    val smbConnections by viewModel.smbConnections.collectAsState()
    val trashCount by viewModel.trashCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var connectionToDelete by remember { mutableStateOf<SmbConnection?>(null) }
    var menuExpandedConnectionId by remember { mutableStateOf<Long?>(null) }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FilePrivacy",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            // 1. Section Stockage
            item {
                Text(
                    text = "Stockage",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = Spacing.medium, bottom = Spacing.small)
                )
            }

            item {
                SourceCard(
                    title = "Stockage interne",
                    subtitle = "Mémoire du téléphone",
                    icon = Icons.Outlined.PhoneAndroid,
                    isEnabled = true,
                    storageSpaceInfo = internalSpaceInfo,
                    onClick = {
                        navController.navigate(NavRoutes.fileListRoute("local", internalRootPath))
                    }
                )
            }

            if (externalVolumes.isEmpty()) {
                item {
                    SourceCard(
                        title = "Aucun stockage externe détecté",
                        subtitle = "Branchez une carte SD ou une clé USB",
                        icon = Icons.Outlined.SdCard,
                        isEnabled = false,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Aucun stockage externe détecté")
                            }
                        }
                    )
                }
            } else {
                items(externalVolumes, key = { it.path }) { volume ->
                    val icon = if (volume.name.contains("USB", ignoreCase = true)) {
                        Icons.Outlined.Usb
                    } else {
                        Icons.Outlined.SdCard
                    }
                    val spaceInfo = remember(volume.path) {
                        StorageVolumesHelper.getStorageSpaceInfo(volume.path)
                    }
                    SourceCard(
                        title = volume.name,
                        subtitle = volume.path,
                        icon = icon,
                        isEnabled = true,
                        storageSpaceInfo = spaceInfo,
                        onClick = {
                            navController.navigate(NavRoutes.fileListRoute("external", volume.path))
                        }
                    )
                }
            }

            // 2. Section Utilitaires
            item {
                Spacer(modifier = Modifier.height(Spacing.medium))
                Text(
                    text = "Utilitaires",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = Spacing.medium, bottom = Spacing.small)
                )
            }

            item {
                SourceCard(
                    title = "Corbeille",
                    subtitle = if (trashCount > 0) "$trashCount élément(s)" else "Vide",
                    icon = Icons.Outlined.Delete,
                    isEnabled = true,
                    onClick = {
                        navController.navigate(NavRoutes.TRASH)
                    }
                )
            }

            // 3. Section Connexions réseau
            item {
                Spacer(modifier = Modifier.height(Spacing.medium))
                Text(
                    text = "Connexions réseau",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = Spacing.medium, bottom = Spacing.small)
                )
            }

            if (smbConnections.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.card),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.medium),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Aucune connexion configurée",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.medium))
                            OutlinedButton(
                                onClick = {
                                    navController.navigate(NavRoutes.ADD_SMB_CONNECTION)
                                },
                                shape = RoundedCornerShape(Radius.pill)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text("Ajouter une connexion", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                items(smbConnections, key = { it.id }) { connection ->
                    Box {
                        SourceCard(
                            title = connection.name,
                            subtitle = "${connection.serverAddress}/${connection.shareName}",
                            icon = Icons.Outlined.Dns,
                            isEnabled = true,
                            onClick = {
                                navController.navigate(NavRoutes.smbListRoute(connection.id, ""))
                            },
                            onLongClick = {
                                menuExpandedConnectionId = connection.id
                            }
                        )

                        DropdownMenu(
                            expanded = (menuExpandedConnectionId == connection.id),
                            onDismissRequest = { menuExpandedConnectionId = null },
                            shape = RoundedCornerShape(Radius.item)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Supprimer") },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpandedConnectionId = null
                                    connectionToDelete = connection
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedButton(
                            onClick = {
                                navController.navigate(NavRoutes.ADD_SMB_CONNECTION)
                            },
                            shape = RoundedCornerShape(Radius.pill)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.small))
                            Text("Ajouter une connexion", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    connectionToDelete?.let { connection ->
        DeleteConfirmationDialog(
            itemName = connection.name,
            onDismiss = { connectionToDelete = null },
            onConfirm = {
                connectionToDelete = null
                viewModel.deleteConnection(connection)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SourceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    storageSpaceInfo: StorageSpaceInfo? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .alpha(if (isEnabled) 1.0f else 0.5f),
        shape = RoundedCornerShape(Radius.card),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = if (isEnabled) 3.dp else 0.dp,
        tonalElevation = if (isEnabled) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 4.dp)
            )

            Spacer(modifier = Modifier.width(Spacing.medium))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (storageSpaceInfo != null && storageSpaceInfo.totalBytes > 0) {
                    val usedFormatted = humanReadableByteCountSI(storageSpaceInfo.usedBytes)
                    val totalFormatted = humanReadableByteCountSI(storageSpaceInfo.totalBytes)
                    val progress = (storageSpaceInfo.usedBytes.toFloat() / storageSpaceInfo.totalBytes.toFloat()).coerceIn(0f, 1f)

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$usedFormatted utilisés sur $totalFormatted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}
