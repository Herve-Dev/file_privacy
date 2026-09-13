package com.hervedev.fileprivacy.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.ui.theme.Radius
import com.hervedev.fileprivacy.ui.utils.formatLastModified
import com.hervedev.fileprivacy.ui.utils.getFileExtension
import com.hervedev.fileprivacy.ui.utils.humanReadableByteCountSI
import java.io.File

@Composable
fun FileDetailsDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    val directItemCount = remember(item) {
        if (item.isDirectory) {
            val file = File(item.path)
            file.listFiles()?.size ?: 0
        } else {
            0
        }
    }

    val extension = remember(item) {
        if (item.isDirectory) "Aucune" else getFileExtension(item.name)
    }

    val formattedDate = remember(item.lastModified) {
        formatLastModified(item.lastModified)
    }

    AlertDialog(
        shape = RoundedCornerShape(Radius.dialog),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow(label = "Type", value = if (item.isDirectory) "Dossier" else "Fichier")

                if (!item.isDirectory) {
                    Spacer(modifier = Modifier.height(6.dp))
                    DetailRow(label = "Extension", value = if (extension != "Aucune") ".$extension" else "Aucune")
                    Spacer(modifier = Modifier.height(6.dp))
                    DetailRow(label = "Taille", value = humanReadableByteCountSI(item.sizeBytes))
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    DetailRow(label = "Contenu", value = "$directItemCount élément(s)")
                }

                Spacer(modifier = Modifier.height(6.dp))
                DetailRow(label = "Modifié le", value = formattedDate)

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Chemin d'accès :",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                SelectionContainer {
                    Text(
                        text = item.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
