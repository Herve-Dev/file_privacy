package com.hervedev.fileprivacy.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hervedev.fileprivacy.ui.AddSmbConnectionScreen
import com.hervedev.fileprivacy.ui.FileListScreen
import com.hervedev.fileprivacy.ui.HomeScreen

object NavRoutes {
    const val HOME = "home"
    const val ADD_SMB_CONNECTION = "addSmbConnection"
    const val FILE_LIST = "fileList/{sourceType}/{encodedPath}"

    fun fileListRoute(sourceType: String, path: String): String =
        "fileList/$sourceType/${Uri.encode(path)}"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        modifier = modifier
    ) {
        composable(route = NavRoutes.HOME) {
            HomeScreen(navController = navController)
        }

        composable(route = NavRoutes.ADD_SMB_CONNECTION) {
            AddSmbConnectionScreen(navController = navController)
        }

        composable(
            route = NavRoutes.FILE_LIST,
            arguments = listOf(
                navArgument("sourceType") { type = NavType.StringType },
                navArgument("encodedPath") { type = NavType.StringType }
            )
        ) {
            FileListScreen(
                navController = navController
            )
        }
    }
}
