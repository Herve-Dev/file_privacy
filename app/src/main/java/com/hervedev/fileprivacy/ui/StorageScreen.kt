package com.hervedev.fileprivacy.ui

import android.os.Environment
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hervedev.fileprivacy.data.FileCategory
import com.hervedev.fileprivacy.data.StorageSpaceInfo
import com.hervedev.fileprivacy.data.StorageVolumesHelper
import com.hervedev.fileprivacy.ui.components.AppCard
import com.hervedev.fileprivacy.ui.navigation.NavRoutes
import com.hervedev.fileprivacy.ui.theme.FileTypeBadges
import com.hervedev.fileprivacy.ui.theme.Radius
import com.hervedev.fileprivacy.ui.theme.Spacing
import com.hervedev.fileprivacy.ui.utils.humanReadableByteCountSI
import com.hervedev.fileprivacy.ui.viewmodel.CategorySizeInfo
import com.hervedev.fileprivacy.ui.viewmodel.StorageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    navController: NavController,
    viewModel: StorageViewModel = viewModel()
) {
    val externalVolumes by viewModel.externalVolumes.collectAsState()
    val internalSpaceInfo by viewModel.internalSpaceInfo.collectAsState()
    val categorySizes by viewModel.categorySizes.collectAsState()
    val isLoadingChart by viewModel.isLoadingChart.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var needsBackgroundRefresh by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                needsBackgroundRefresh = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshVolumes()
                if (needsBackgroundRefresh) {
                    viewModel.invalidateCache()
                    viewModel.loadBreakdown(force = true)
                    needsBackgroundRefresh = false
                } else {
                    viewModel.loadBreakdown(force = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val internalRootPath = remember { Environment.getExternalStorageDirectory().absolutePath }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Stockage",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isLoadingChart,
            onRefresh = {
                viewModel.refreshVolumes()
                viewModel.loadBreakdown(force = true)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                // 1. Liste des sources de stockage (Stockage interne + volumes externes)
                item {
                    Text(
                        text = "Sources de stockage",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    StorageVolumeCard(
                        title = "Stockage interne",
                        subtitle = "Mémoire du téléphone",
                        icon = Icons.Outlined.PhoneAndroid,
                        storageSpaceInfo = internalSpaceInfo,
                        onClick = {
                            navController.navigate(NavRoutes.fileListRoute("local", internalRootPath))
                        }
                    )
                }

                if (externalVolumes.isNotEmpty()) {
                    items(externalVolumes, key = { it.path }) { volume ->
                        val icon = if (volume.name.contains("USB", ignoreCase = true)) {
                            Icons.Outlined.Usb
                        } else {
                            Icons.Outlined.SdCard
                        }
                        val spaceInfo = remember(volume.path) {
                            StorageVolumesHelper.getStorageSpaceInfo(volume.path)
                        }
                        StorageVolumeCard(
                            title = volume.name,
                            subtitle = volume.path,
                            icon = icon,
                            storageSpaceInfo = spaceInfo,
                            onClick = {
                                navController.navigate(NavRoutes.fileListRoute("external", volume.path))
                            }
                        )
                    }
                }

                // 2. Répartition par catégorie (donut chart)
                item {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        text = "Répartition par catégorie",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                            if (isLoadingChart && categorySizes.isEmpty()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(Spacing.medium)
                                )
                            } else {
                                val totalUsedBytes = internalSpaceInfo?.usedBytes ?: 0L

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StorageDonutChart(
                                        categorySizes = categorySizes,
                                        totalUsedBytes = totalUsedBytes
                                    )

                                    Column(
                                        modifier = Modifier.padding(start = Spacing.medium)
                                    ) {
                                        Text(
                                            text = "Utilisé",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = humanReadableByteCountSI(totalUsedBytes),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(Spacing.medium))
                                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(Spacing.medium))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                                ) {
                                    categorySizes.forEach { info ->
                                        CategoryBreakdownRow(info = info)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageVolumeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    storageSpaceInfo: StorageSpaceInfo?,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Radius.card)
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 4.dp)
            )

            Spacer(modifier = Modifier.width(Spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
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

@Composable
private fun StorageDonutChart(
    categorySizes: List<CategorySizeInfo>,
    totalUsedBytes: Long,
    modifier: Modifier = Modifier
) {
    val defaultColor = MaterialTheme.colorScheme.primary
    val categoryColors = mapOf(
        FileCategory.IMAGES to FileTypeBadges.ImageAccent,
        FileCategory.VIDEOS to FileTypeBadges.VideoAccent,
        FileCategory.AUDIO to FileTypeBadges.AudioAccent,
        FileCategory.DOCUMENTS to FileTypeBadges.DocumentAccent,
        FileCategory.DOWNLOADS to FileTypeBadges.FolderAccent,
        FileCategory.APK to FileTypeBadges.ApkAccent
    )
    val otherColor = Color(0xFFABABAB)

    if (totalUsedBytes <= 0L) return

    Canvas(modifier = modifier.size(130.dp)) {
        val strokeWidth = 20.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val arcSize = Size(diameter, diameter)

        var startAngle = -90f

        categorySizes.forEach { info ->
            if (info.sizeBytes > 0) {
                val sweepAngle = (info.sizeBytes.toFloat() / totalUsedBytes.toFloat()) * 360f
                val color = if (info.category != null) {
                    categoryColors[info.category] ?: defaultColor
                } else {
                    otherColor
                }

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(info: CategorySizeInfo) {
    val defaultColor = MaterialTheme.colorScheme.primary
    val categoryColors = mapOf(
        FileCategory.IMAGES to FileTypeBadges.ImageAccent,
        FileCategory.VIDEOS to FileTypeBadges.VideoAccent,
        FileCategory.AUDIO to FileTypeBadges.AudioAccent,
        FileCategory.DOCUMENTS to FileTypeBadges.DocumentAccent,
        FileCategory.DOWNLOADS to FileTypeBadges.FolderAccent,
        FileCategory.APK to FileTypeBadges.ApkAccent
    )
    val color = if (info.category != null) {
        categoryColors[info.category] ?: defaultColor
    } else {
        Color(0xFFABABAB)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(Spacing.small))
            Text(
                text = info.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = humanReadableByteCountSI(info.sizeBytes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
