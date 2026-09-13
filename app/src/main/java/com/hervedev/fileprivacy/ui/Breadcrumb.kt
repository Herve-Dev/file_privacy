package com.hervedev.fileprivacy.ui

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

data class BreadcrumbItem(
    val name: String,
    val path: String,
    val isRoot: Boolean = false,
    val isCurrent: Boolean = false
)

fun parseBreadcrumbItems(
    currentPath: String,
    rootPath: String = Environment.getExternalStorageDirectory().absolutePath
): List<BreadcrumbItem> {
    val items = mutableListOf<BreadcrumbItem>()

    val normalizedCurrent = try { File(currentPath).canonicalPath } catch (_: Exception) { currentPath }
    val normalizedRoot = try { File(rootPath).canonicalPath } catch (_: Exception) { rootPath }

    val isAtRoot = (normalizedCurrent == normalizedRoot)
    items.add(
        BreadcrumbItem(
            name = "Accueil",
            path = normalizedRoot,
            isRoot = true,
            isCurrent = isAtRoot
        )
    )

    if (normalizedCurrent.startsWith(normalizedRoot) && normalizedCurrent != normalizedRoot) {
        val relative = normalizedCurrent.substring(normalizedRoot.length).removePrefix("/")
        val segments = relative.split("/").filter { it.isNotEmpty() }

        var accumPath = normalizedRoot
        for (i in segments.indices) {
            val segment = segments[i]
            accumPath = "$accumPath/$segment"
            val isLast = (i == segments.lastIndex)
            items.add(
                BreadcrumbItem(
                    name = segment,
                    path = accumPath,
                    isRoot = false,
                    isCurrent = isLast
                )
            )
        }
    } else if (!normalizedCurrent.startsWith(normalizedRoot)) {
        val segments = normalizedCurrent.split("/").filter { it.isNotEmpty() }
        var accumPath = ""
        for (i in segments.indices) {
            val segment = segments[i]
            accumPath = "$accumPath/$segment"
            val isLast = (i == segments.lastIndex)
            items.add(
                BreadcrumbItem(
                    name = segment,
                    path = accumPath,
                    isRoot = false,
                    isCurrent = isLast
                )
            )
        }
    }

    return items
}

@Composable
fun BreadcrumbBar(
    currentPath: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    rootPath: String = Environment.getExternalStorageDirectory().absolutePath
) {
    val items = parseBreadcrumbItems(currentPath, rootPath)
    val listState = rememberLazyListState()

    LaunchedEffect(currentPath) {
        if (items.isNotEmpty()) {
            listState.animateScrollToItem(items.lastIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        itemsIndexed(items, key = { _, item -> item.path }) { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (index > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(enabled = !item.isCurrent) { onItemClick(item.path) }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    if (item.isRoot) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Racine",
                            tint = if (item.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (item.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
