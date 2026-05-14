package com.blogspot.yotsudev.prompttile


import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt の DI グラフを初期化するアプリケーションクラス。
 * @HiltAndroidApp を付けるだけで、Hilt がコンパイル時にコンポーネントを生成する。
 */
@HiltAndroidApp
class PromptTileApp : Application()