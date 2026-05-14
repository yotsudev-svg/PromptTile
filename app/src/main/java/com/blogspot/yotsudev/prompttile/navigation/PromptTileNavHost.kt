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

@Composable
fun PromptTileNavHost(
    settingsViewModel: SettingsViewModel,
    promptViewModel: PromptViewModel,       // ← 追加
) {
    val backStack = rememberAppDestinationBackStack(AppDestination.Main)
    val currentDestination = backStack.lastOrNull()

    Scaffold(
        bottomBar = {
            PromptTileBottomBar(
                currentDestination = currentDestination,
                onItemSelected = { destination ->
                    if (destination == currentDestination) return@PromptTileBottomBar

                    val existingIndex = backStack.indexOfLast { it == destination }
                    if (existingIndex >= 0) {
                        while (backStack.last() != destination) {
                            backStack.removeLastOrNull()
                        }
                    } else {
                        while (backStack.size > 1) {
                            backStack.removeLastOrNull()
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
                    backStack.removeAt(backStack.size - 1)
                }
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
                        viewModel       = settingsViewModel,
                        promptViewModel = promptViewModel,  // ← 追加
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
            NavigationBarItem(
                selected = currentDestination == item.destination,
                onClick = { onItemSelected(item.destination) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
            )
        }
    }
}