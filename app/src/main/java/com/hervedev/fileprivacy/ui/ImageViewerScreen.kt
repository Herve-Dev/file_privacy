package com.hervedev.fileprivacy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hervedev.fileprivacy.domain.FileItem
import com.hervedev.fileprivacy.ui.dialogs.DeleteConfirmationDialog
import com.hervedev.fileprivacy.ui.dialogs.FileDetailsDialog
import com.hervedev.fileprivacy.ui.dialogs.RenameDialog
import com.hervedev.fileprivacy.ui.theme.Radius
import com.hervedev.fileprivacy.ui.theme.Spacing
import com.hervedev.fileprivacy.ui.viewmodel.ImageViewerViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    navController: NavController,
    viewModel: ImageViewerViewModel = viewModel()
) {
    val imagesList by viewModel.imagesList.collectAsState()
    val initialIndex = viewModel.initialIndex
    val sourceType = viewModel.sourceType
    val cachedImageMap by viewModel.cachedImageMap.collectAsState()
    val loadingPages by viewModel.loadingPages.collectAsState()
    val isLocal = sourceType in listOf("local", "external")

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (imagesList.isEmpty()) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    val safeInitialIndex = initialIndex.coerceIn(0, imagesList.size - 1)
    val pagerState = rememberPagerState(initialPage = safeInitialIndex) { imagesList.size }

    var showTopBar by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemToTrash by remember { mutableStateOf<FileItem?>(null) }
    var itemToPermanentlyDelete by remember { mutableStateOf<FileItem?>(null) }
    var itemForDetails by remember { mutableStateOf<FileItem?>(null) }

    val currentItem = imagesList.getOrNull(pagerState.currentPage) ?: imagesList.first()

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AnimatedVisibility(
                visible = showTopBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    contentColor = Color.White
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = currentItem.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${imagesList.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Retour",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color.White
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    shape = RoundedCornerShape(Radius.card)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Renommer") },
                                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            itemToRename = currentItem
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Infos") },
                                        leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            itemForDetails = currentItem
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
                                                showMenu = false
                                                itemToTrash = currentItem
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
                                                showMenu = false
                                                itemToPermanentlyDelete = currentItem
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
                                                showMenu = false
                                                itemToTrash = currentItem
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            userScrollEnabled = !isCurrentPageZoomed
        ) { page ->
            val item = imagesList.getOrNull(page) ?: return@HorizontalPager

            val imageFile: File? = if (isLocal) File(item.path) else cachedImageMap[item.path]
            val isLoadingPage = !isLocal && (imageFile == null || loadingPages.contains(item.path))

            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            LaunchedEffect(page) {
                scale = 1f
                offset = Offset.Zero
                if (page == pagerState.currentPage) {
                    isCurrentPageZoomed = false
                }
                if (!isLocal) {
                    viewModel.ensureImageCached(item)
                }
            }

            LaunchedEffect(scale) {
                if (page == pagerState.currentPage) {
                    isCurrentPageZoomed = scale > 1.05f
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(page) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val pointerCount = event.changes.size

                                if (pointerCount >= 2 || scale > 1.01f) {
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()

                                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                                    scale = newScale

                                    if (newScale > 1f) {
                                        val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                        val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                                        val newOffsetX = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                        val newOffsetY = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                        offset = Offset(newOffsetX, newOffsetY)
                                    } else {
                                        offset = Offset.Zero
                                    }

                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showTopBar = !showTopBar
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoadingPage) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Text(
                            text = "Chargement...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                } else if (imageFile != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
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
            title = "Supprimer l'image ?",
            message = "Voulez-vous déplacer \"${item.name}\" vers la corbeille ?",
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
            message = "Voulez-vous vraiment supprimer définitivement \"${item.name}\" ? Cette action est irréversible.",
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

    itemForDetails?.let { item ->
        FileDetailsDialog(
            item = item,
            onDismiss = { itemForDetails = null }
        )
    }
}
