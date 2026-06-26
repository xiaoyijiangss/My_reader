package com.myreader.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object BookDetail : Screen("book_detail")
    object Player : Screen("player")
    object SourceManager : Screen("source_manager")
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("书架", Icons.Default.Home, Screen.Library.route),
    BottomNavItem("搜索", Icons.Default.Search, Screen.Search.route),
    BottomNavItem("设置", Icons.Default.Settings, Screen.Settings.route)
)
