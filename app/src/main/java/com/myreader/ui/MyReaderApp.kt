package com.myreader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myreader.ui.navigation.Screen
import com.myreader.ui.navigation.bottomNavItems
import com.myreader.ui.screen.*
import com.myreader.ui.theme.MyReaderTheme
import com.myreader.viewmodel.PlayerViewModel

@Composable
fun MyReaderApp() {
    MyReaderTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // 底部栏是否显示（播放页面隐藏）
        val showBottomBar = currentRoute !in listOf(Screen.Player.route)

        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Library.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Library.route) {
                    LibraryScreen(
                        onBookClick = { bookId ->
                            navController.navigate("${Screen.BookDetail.route}/$bookId/0")
                        }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        onResultClick = { sourceId, sourceUrl ->
                            navController.navigate(
                                "${Screen.BookDetail.route}/-1/0" +
                                    "?sourceId=$sourceId&sourceUrl=${java.net.URLEncoder.encode(sourceUrl, "UTF-8")}"
                            )
                        }
                    )
                }

                composable(
                    route = "${Screen.BookDetail.route}/{bookId}/{isFromLib}?sourceId={sourceId}&sourceUrl={sourceUrl}",
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.LongType },
                        navArgument("isFromLib") { type = NavType.IntType },
                        navArgument("sourceId") { type = NavType.StringType; defaultValue = "" },
                        navArgument("sourceUrl") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getLong("bookId") ?: -1L
                    val isFromLib = backStackEntry.arguments?.getInt("isFromLib") == 1
                    val sourceId = backStackEntry.arguments?.getString("sourceId") ?: ""
                    val sourceUrl = backStackEntry.arguments?.getString("sourceUrl") ?: ""

                    BookDetailScreen(
                        bookId = bookId,
                        isFromLib = isFromLib,
                        incomingSourceId = sourceId,
                        incomingSourceUrl = sourceUrl,
                        onPlay = { book, chapters, index ->
                            navController.navigate(Screen.Player.route)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Player.route) {
                    PlayerScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }
    }
}
