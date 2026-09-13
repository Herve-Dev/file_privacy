package com.hervedev.fileprivacy.ui.navigation

import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hervedev.fileprivacy.ui.FileListScreen

object NavRoutes {
    const val FILE_LIST = "fileList/{encodedPath}"

    fun fileListRoute(path: String): String = "fileList/${Uri.encode(path)}"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val rootPath = Environment.getExternalStorageDirectory().absolutePath

    NavHost(
        navController = navController,
        startDestination = NavRoutes.fileListRoute(rootPath),
        modifier = modifier
    ) {
        composable(
            route = NavRoutes.FILE_LIST,
            arguments = listOf(
                navArgument("encodedPath") { type = NavType.StringType }
            )
        ) {
            FileListScreen(
                navController = navController
            )
        }
    }
}
