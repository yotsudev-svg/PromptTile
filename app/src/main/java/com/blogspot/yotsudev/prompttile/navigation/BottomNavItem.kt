package com.blogspot.yotsudev.prompttile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * BottomNavigationBar の各タブに必要なメタデータをまとめたデータクラス。
 *
 * ルート・ラベル・アイコンを1つのオブジェクトで管理することで、
 * NavHost側とBottomBar側が同じリストを参照でき、定義の重複を防ぐ。
 */
data class BottomNavItem(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector,
)

/** アプリ全体で使うBottomNavのタブ定義。順番がそのまま表示順になる。 */
val bottomNavItems = listOf(
    BottomNavItem(
        destination = AppDestination.Main,
        label = "メイン",
        icon = Icons.Default.Home,
    ),
    BottomNavItem(
        destination = AppDestination.Saved,
        label = "保存済み",
        icon = Icons.Default.Favorite,
    ),
    BottomNavItem(
        destination = AppDestination.Edit,
        label = "編集",
        icon = Icons.Default.Build,
    ),
    BottomNavItem(
        destination = AppDestination.Settings,
        label = "設定",
        icon = Icons.Default.Settings,
    ),
)