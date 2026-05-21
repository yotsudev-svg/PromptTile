package com.blogspot.yotsudev.prompttile.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.blogspot.yotsudev.prompttile.R

/**
 * BottomNavigationBar の各タブに必要なメタデータをまとめたデータクラス。
 *
 * ラベルはリソース ID を保持し、多言語対応と安全な解決を実現します。
 */
data class BottomNavItem(
    val destination: AppDestination,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

/** アプリ全体で使うBottomNavのタブ定義。順番がそのまま表示順になる。 */
val bottomNavItems = listOf(
    BottomNavItem(
        destination = AppDestination.Main,
        labelRes = R.string.nav_main,
        icon = Icons.Default.Home,
    ),
    BottomNavItem(
        destination = AppDestination.Saved,
        labelRes = R.string.nav_saved,
        icon = Icons.Default.Favorite,
    ),
    BottomNavItem(
        destination = AppDestination.Edit,
        labelRes = R.string.nav_edit,
        icon = Icons.Default.Build,
    ),
    BottomNavItem(
        destination = AppDestination.Settings,
        labelRes = R.string.nav_settings,
        icon = Icons.Default.Settings,
    ),
)
