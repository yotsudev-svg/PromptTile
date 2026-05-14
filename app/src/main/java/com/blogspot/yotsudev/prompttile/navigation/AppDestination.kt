package com.blogspot.yotsudev.prompttile.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * アプリ内の全ルートを sealed interface で一元管理する。
 *
 * Navigation 3 では以下の2点が必須：
 * - NavKey を実装 → ライブラリがバックスタックのキーとして認識できる
 * - @Serializable → 画面回転・プロセス再起動時に状態を復元できる
 */
@Serializable
sealed interface AppDestination : NavKey {

    @Serializable
    data object Main : AppDestination

    @Serializable
    data object Saved : AppDestination

    @Serializable
    data object Edit : AppDestination

    @Serializable
    data object Settings : AppDestination
}