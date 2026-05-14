package com.blogspot.yotsudev.prompttile.di

import com.blogspot.yotsudev.prompttile.data.preferences.PreferencesDataSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * PreferencesDataSource は @Singleton + @Inject constructor で定義済みのため、
 * Hilt が自動でDIグラフに登録する。
 * 追加の @Module / @Provides は不要。
 *
 * このファイルは将来的に DataStore 関連の設定を追加する場合の置き場として残す。
 */