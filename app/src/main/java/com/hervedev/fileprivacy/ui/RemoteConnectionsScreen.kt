package com.hervedev.fileprivacy.ui

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
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hervedev.fileprivacy.domain.SmbConnection
import com.hervedev.fileprivacy.ui.components.AppCard
import com.hervedev.fileprivacy.ui.dialogs.DeleteConfirmationDialog
import com.hervedev.fileprivacy.ui.navigation.NavRoutes
import com.hervedev.fileprivacy.ui.theme.Radius
import com.hervedev.fileprivacy.ui.theme.Spacing
import com.hervedev.fileprivacy.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RemoteConnectionsScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val smbConnections by viewModel.smbConnections.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var connectionToDelete by remember { mutableStateOf<SmbConnection?>(null) }
    var menuExpandedConnectionId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Distant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // Section 1: SMB
            item {
                Text(
                    text = "Partages SMB",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (smbConnections.isEmpty()) {
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.card)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.medium),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Aucune connexion SMB configurée",
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
                                Text("Ajouter une connexion SMB", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                items(smbConnections, key = { it.id }) { connection ->
                    Box {
                        AppCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.card))
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate(NavRoutes.smbListRoute(connection.id, ""))
                                    },
                                    onLongClick = {
                                        menuExpandedConnectionId = connection.id
                                    }
                                ),
                            shape = RoundedCornerShape(Radius.card)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.medium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Dns,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(end = 4.dp)
                                )

                                Spacer(modifier = Modifier.width(Spacing.medium))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = connection.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${connection.serverAddress}/${connection.shareName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = (menuExpandedConnectionId == connection.id),
                            onDismissRequest = { menuExpandedConnectionId = null },
                            shape = RoundedCornerShape(Radius.card)
                        ) {
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
                                    menuExpandedConnectionId = null
                                    connectionToDelete = connection
                                }
                            )
                        }
                    }
                }

                item {
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
                            Text("Ajouter une connexion SMB", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Section 2: FTP / SFTP (préparation visuelle)
            item {
                Spacer(modifier = Modifier.height(Spacing.small))
                Text(
                    text = "FTP / SFTP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.card)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.medium))
                        Column {
                            Text(
                                text = "FTP / SFTP",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Disponible prochainement",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Section 3: WebDAV (préparation visuelle)
            item {
                Spacer(modifier = Modifier.height(Spacing.small))
                Text(
                    text = "WebDAV",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.card)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.medium))
                        Column {
                            Text(
                                text = "WebDAV",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Disponible prochainement",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
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
            title = "Supprimer la connexion ?",
            message = "Voulez-vous vraiment supprimer la connexion \"${connection.name}\" ?",
            onDismiss = { connectionToDelete = null },
            onConfirm = {
                connectionToDelete = null
                viewModel.deleteConnection(connection)
            }
        )
    }
}
