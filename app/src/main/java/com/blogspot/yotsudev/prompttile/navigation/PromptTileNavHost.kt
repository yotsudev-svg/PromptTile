package com.blogspot.yotsudev.prompttile.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

// ---- 拡張関数 ------------------------------------------------
//
// BottomNav タップ時のバックスタック操作を切り出した。
// ロジックは以下の2パターン:
//   1. 既にスタックにある → その位置まで pop して戻る（履歴を維持）
//   2. まだスタックにない → Main だけ残して新しい画面を push
//
// NavBackStack は minSdk 30 対応のため subList().clear() が使えず
// removeAt() のループで実装している。拡張関数にすることで
// PromptTileNavHost 本体からこの複雑さを隠蔽できる。

private fun NavBackStack<AppDestination>.navigateTo(destination: AppDestination) {
    val existingIndex = indexOf(destination)
    if (existingIndex >= 0) {
        // すでにスタックにある場合: その位置より上を全て pop
        val itemsToRemove = size - 1 - existingIndex
        repeat(itemsToRemove) { removeAt(lastIndex) }
    } else {
        // スタックにない場合: Main（index=0）だけ残して push
        while (size > 1) removeAt(lastIndex)
        if (destination != AppDestination.Main) add(destination)
    }
}

@Composable
fun PromptTileNavHost(
    settingsViewModel: SettingsViewModel,
    promptViewModel: PromptViewModel,
) {
    val backStack = rememberAppDestinationBackStack(AppDestination.Main)
    val currentDestination = backStack.lastOrNull()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            PromptTileBottomBar(
                currentDestination = currentDestination,
                onItemSelected = { destination ->
                    // 現在のタブを再タップした場合は何もしない
                    if (destination == currentDestination) return@PromptTileBottomBar
                    backStack.navigateTo(destination)
                },
            )
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            onBack = {
                if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<AppDestination.Main>     { MainScreen(viewModel = promptViewModel) }
                entry<AppDestination.Saved>    { SavedScreen(promptViewModel = promptViewModel) }
                entry<AppDestination.Edit>     { EditScreen() }
                entry<AppDestination.Settings> {
                    SettingsScreen(
                        viewModel = settingsViewModel
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
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
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