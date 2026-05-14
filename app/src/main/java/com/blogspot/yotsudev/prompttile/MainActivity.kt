package com.blogspot.yotsudev.prompttile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.navigation.PromptTileNavHost
import com.blogspot.yotsudev.prompttile.ui.main.PromptViewModel
import com.blogspot.yotsudev.prompttile.ui.settings.SettingsViewModel
import com.blogspot.yotsudev.prompttile.ui.theme.PromptTileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val promptViewModel: PromptViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. super.onCreate() の前にスプラッシュ画面を初期化する
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2. ViewModel の準備（DataStoreのロードなど）が整うまでスプラッシュ画面を維持する
        splashScreen.setKeepOnScreenCondition {
            !settingsViewModel.isReady.value
        }

        setContent {
            // StateFlow を Compose の State として購読
            val prefs by settingsViewModel.preferences.collectAsStateWithLifecycle()

            // 3. prefs が null の間は Compose の描画自体を行わない
            // （この間はスプラッシュ画面が最前面に維持されているため、ユーザーには白画面は見えない）
            val currentPrefs = prefs ?: return@setContent

            PromptTileTheme(
                themeConfig = currentPrefs.themeConfig
            ) {
                PromptTileNavHost(
                    settingsViewModel = settingsViewModel,
                    promptViewModel   = promptViewModel,
                )
            }
        }
    }
}