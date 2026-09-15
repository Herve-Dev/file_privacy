package com.hervedev.fileprivacy.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hervedev.fileprivacy.ui.AddSmbConnectionScreen
import com.hervedev.fileprivacy.ui.FileListScreen
import com.hervedev.fileprivacy.ui.HomeScreen
import com.hervedev.fileprivacy.ui.PlaceholderScreen
import com.hervedev.fileprivacy.ui.RemoteConnectionsScreen
import com.hervedev.fileprivacy.ui.TrashScreen

object NavRoutes {
    const val HOME = "home"
    const val STORAGE = "storage"
    const val RECENTS = "recents"
    const val REMOTE = "remote"
    const val SETTINGS = "settings"

    const val ADD_SMB_CONNECTION = "addSmbConnection"
    const val TRASH = "trash"
    const val FILE_LIST = "fileList/{sourceType}/{encodedPath}"
    const val FILE_LIST_SMB = "fileListSmb/{connectionId}/{encodedPath}"

    fun fileListRoute(sourceType: String, path: String): String =
        "fileList/$sourceType/${Uri.encode(path)}"

    fun smbListRoute(connectionId: Long, relativePath: String): String =
        "fileListSmb/$connectionId/${Uri.encode(relativePath)}"
}

private data class NavTabItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
private fun AppBottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val rootTabs = listOf(
        NavTabItem(NavRoutes.HOME, "Fichiers", Icons.Outlined.Folder),
        NavTabItem(NavRoutes.STORAGE, "Stockage", Icons.Outlined.SdCard),
        NavTabItem(NavRoutes.RECENTS, "Récents", Icons.Outlined.History),
        NavTabItem(NavRoutes.REMOTE, "Distant", Icons.Outlined.Cloud),
        NavTabItem(NavRoutes.SETTINGS, "Réglages", Icons.Outlined.Settings)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        rootTabs.forEach { tab ->
            val selected = (currentRoute == tab.route)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val rootRoutes = listOf(
        NavRoutes.HOME,
        NavRoutes.STORAGE,
        NavRoutes.RECENTS,
        NavRoutes.REMOTE,
        NavRoutes.SETTINGS
    )
    val isRootScreen = currentRoute in rootRoutes

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isRootScreen) {
                AppBottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = NavRoutes.HOME) {
                HomeScreen(navController = navController)
            }

            composable(route = NavRoutes.STORAGE) {
                PlaceholderScreen(title = "Stockage")
            }

            composable(route = NavRoutes.RECENTS) {
                PlaceholderScreen(title = "Récents")
            }

            composable(route = NavRoutes.REMOTE) {
                RemoteConnectionsScreen(navController = navController)
            }

            composable(route = NavRoutes.SETTINGS) {
                PlaceholderScreen(title = "Réglages")
            }

            composable(route = NavRoutes.ADD_SMB_CONNECTION) {
                AddSmbConnectionScreen(navController = navController)
            }

            composable(route = NavRoutes.TRASH) {
                TrashScreen(navController = navController)
            }

            composable(
                route = NavRoutes.FILE_LIST,
                arguments = listOf(
                    navArgument("sourceType") { type = NavType.StringType },
                    navArgument("encodedPath") { type = NavType.StringType }
                )
            ) {
                FileListScreen(navController = navController)
            }

            composable(
                route = NavRoutes.FILE_LIST_SMB,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.LongType },
                    navArgument("encodedPath") { type = NavType.StringType }
                )
            ) {
                FileListScreen(navController = navController)
            }
        }
    }
}
