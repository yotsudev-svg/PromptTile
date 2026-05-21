package com.blogspot.yotsudev.prompttile.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.blogspot.yotsudev.prompttile.ui.edit.EditScreen
import com.blogspot.yotsudev.prompttile.ui.main.MainScreen
import com.blogspot.yotsudev.prompttile.ui.main.PromptViewModel
import com.blogspot.yotsudev.prompttile.ui.saved.SavedScreen
import com.blogspot.yotsudev.prompttile.ui.settings.SettingsScreen
import com.blogspot.yotsudev.prompttile.ui.settings.SettingsViewModel
import kotlinx.serialization.serializer

@Composable
fun rememberAppDestinationBackStack(vararg elements: AppDestination): NavBackStack<AppDestination> {
    return rememberSerializable(serializer = serializer()) {
        NavBackStack(*elements)
    }
}

/**
 * アプリのメインナビゲーションホスト。
 * Navigation 3 を使用し、各画面の遷移とバックスタックを管理します。
 */
@Composable
fun PromptTileNavHost(
    settingsViewModel: SettingsViewModel,
    promptViewModel: PromptViewModel,
) {
    val backStack = rememberAppDestinationBackStack(AppDestination.Main)
    val currentDestination = backStack.lastOrNull()

    Scaffold(
        bottomBar = {
            PromptTileBottomBar(
                currentDestination = currentDestination,
                onItemSelected = { destination ->
                    if (destination == currentDestination) return@PromptTileBottomBar

                    // バックスタック管理の最適化 (minSdk 30 対応のため removeAt を使用)
                    val existingIndex = backStack.indexOf(destination)
                    if (existingIndex >= 0) {
                        // 既にスタックにある場合は、その位置まで戻す
                        val itemsToRemove = backStack.size - 1 - existingIndex
                        repeat(itemsToRemove) { backStack.removeAt(backStack.lastIndex) }
                    } else {
                        // メイン(root)以外をクリアして新しい画面を積む
                        while (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                        if (destination != AppDestination.Main) {
                            backStack.add(destination)
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                // 画面ごとに必要な ViewModel を注入。
                // 共有が必要な promptViewModel はそのまま渡し、
                // 画面固有の ViewModel は各 Screen 内で hiltViewModel() を使って取得する。
                entry<AppDestination.Main>     { MainScreen(viewModel = promptViewModel) }
                entry<AppDestination.Saved>    { SavedScreen(promptViewModel = promptViewModel) }
                entry<AppDestination.Edit>     { EditScreen() }
                entry<AppDestination.Settings> { 
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        promptViewModel = promptViewModel
                    ) 
                }
            },
        )
    }
}

@Composable
private fun PromptTileBottomBar(
    currentDestination: AppDestination?,
    onItemSelected: (AppDestination) -> Unit,
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val label = stringResource(item.labelRes)
            NavigationBarItem(
                selected = currentDestination == item.destination,
                onClick = { onItemSelected(item.destination) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = label,
                    )
                },
                label = { Text(label) },
            )
        }
    }
}
