package com.hervedev.fileprivacy.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hervedev.fileprivacy.ui.components.AppCard
import com.hervedev.fileprivacy.ui.theme.Radius
import com.hervedev.fileprivacy.ui.theme.Spacing
import com.hervedev.fileprivacy.ui.viewmodel.AddWebDavConnectionViewModel
import com.hervedev.fileprivacy.ui.viewmodel.TestConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWebDavConnectionScreen(
    navController: NavController,
    viewModel: AddWebDavConnectionViewModel = viewModel()
) {
    val name by viewModel.name.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val port by viewModel.port.collectAsState()
    val username by viewModel.username.collectAsState()
    val basePath by viewModel.basePath.collectAsState()
    val password by viewModel.password.collectAsState()
    val testState by viewModel.testState.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var showBasePathInfoDialog by remember { mutableStateOf(false) }

    val canSave = serverUrl.isNotBlank() && (testState !is TestConnectionState.Testing)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Connexion WebDAV",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Retour"
                        )
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
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.card)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = Spacing.small)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudQueue,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.medium))
                            Column {
                                Text(
                                    text = "Paramètres de connexion WebDAV",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Serveur Nextcloud, ownCloud ou WebDAV générique",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(Spacing.extraSmall))

                        // Nom
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                viewModel.name.value = it
                                viewModel.resetTestState()
                            },
                            label = { Text("Nom de la connexion (optionnel)") },
                            placeholder = { Text("Ex: Mon Nextcloud") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Serveur
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = {
                                viewModel.onServerUrlChange(it)
                                viewModel.resetTestState()
                            },
                            label = { Text("Adresse du serveur *") },
                            placeholder = { Text("Ex: http://192.168.1.50 ou https://cloud.exemple.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Port
                        OutlinedTextField(
                            value = port,
                            onValueChange = {
                                viewModel.onPortChange(it)
                                viewModel.resetTestState()
                            },
                            label = { Text("Port") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Utilisateur
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                viewModel.onUsernameChange(it)
                                viewModel.resetTestState()
                            },
                            label = { Text("Nom d'utilisateur") },
                            placeholder = { Text("Ex: herve") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Chemin WebDAV
                        OutlinedTextField(
                            value = basePath,
                            onValueChange = {
                                viewModel.onBasePathChange(it)
                                viewModel.resetTestState()
                            },
                            label = { Text("Chemin WebDAV") },
                            placeholder = { Text("Ex: /remote.php/dav/files/herve/") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showBasePathInfoDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "Info chemin WebDAV",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Mot de passe
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                viewModel.password.value = it
                                viewModel.resetTestState()
                            },
                            label = { Text("Mot de passe") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "Masquer" else "Afficher"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Boutons d'action & Etat du test
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    // Test Result Banner
                    when (val state = testState) {
                        is TestConnectionState.Testing -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text(
                                    text = "Test de la connexion en cours...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        is TestConnectionState.Success -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text(
                                    text = "Connexion réussie !",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        is TestConnectionState.Error -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(Spacing.small))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {}
                    }

                    // Bouton Tester
                    OutlinedButton(
                        onClick = { viewModel.testConnection() },
                        enabled = serverUrl.isNotBlank() && testState !is TestConnectionState.Testing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.pill)
                    ) {
                        Text("Tester la connexion")
                    }

                    // Bouton Enregistrer
                    Button(
                        onClick = {
                            viewModel.saveConnection {
                                navController.popBackStack()
                            }
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Radius.pill)
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }

    if (showBasePathInfoDialog) {
        AlertDialog(
            onDismissRequest = { showBasePathInfoDialog = false },
            title = {
                Text(
                    text = "Chemin WebDAV de base",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Par défaut, le chemin Nextcloud standard est pré-rempli. Modifiez ce champ si vous vous connectez à un autre type de serveur WebDAV.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showBasePathInfoDialog = false }) {
                    Text("Compris")
                }
            },
            shape = RoundedCornerShape(Radius.card)
        )
    }
}
