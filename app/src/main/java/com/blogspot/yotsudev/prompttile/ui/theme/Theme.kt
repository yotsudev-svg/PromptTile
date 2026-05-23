package com.blogspot.yotsudev.prompttile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.view.WindowCompat
import com.blogspot.yotsudev.prompttile.data.preferences.ThemeConfig

private val DarkColorScheme = darkColorScheme(
    primary = AiPurple80,
    secondary = AiEmerald80,
    tertiary = AiCyan80,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF25232D), // 少し明るい紫黒
    onPrimary = AiPurpleDark,
    onSecondary = AiPurpleDark,
    onTertiary = AiPurpleDark,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    onSurfaceVariant = Color(0xFFCAC4D0),
    primaryContainer = AiPurple40,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = AiEmerald40,
    onSecondaryContainer = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = AiCyan40,
    onTertiaryContainer = androidx.compose.ui.graphics.Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = AiPurple40,
    secondary = AiEmerald40,
    tertiary = AiCyan40,
    background = LightBackground,
    surface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = Color(0xFFE7E0EC),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = AiPurple80,
    onPrimaryContainer = AiPurpleDark,
    secondaryContainer = AiEmerald80,
    onSecondaryContainer = AiPurpleDark,
    tertiaryContainer = AiCyan80,
    onTertiaryContainer = AiPurpleDark,
)

@Composable
fun PromptTileTheme(
    themeConfig: ThemeConfig = ThemeConfig.FOLLOW_SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeConfig) {
        ThemeConfig.LIGHT -> false
        ThemeConfig.DARK -> true
        ThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // AIテーマの雰囲気を保つため、背景色とサーフェス色だけは固定色（AI-look）を優先的に採用
            if (darkTheme) {
                baseScheme.copy(
                    background = DarkBackground,
                    surface = DarkSurface,
                    surfaceVariant = Color(0xFF25232D),
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onSurfaceVariant = Color(0xFFCAC4D0),
                )
            } else {
                baseScheme.copy(
                    background = LightBackground,
                    surface = Color.White,
                    surfaceVariant = Color(0xFFE7E0EC),
                )
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.background,
            content = content
        )
    }
}
