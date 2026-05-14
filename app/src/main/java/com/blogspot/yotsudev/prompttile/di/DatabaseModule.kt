package com.blogspot.yotsudev.prompttile.di

import android.content.Context
import androidx.room.Room
import com.blogspot.yotsudev.prompttile.data.db.AppDatabase
import com.blogspot.yotsudev.prompttile.data.dao.CategoryDao
import com.blogspot.yotsudev.prompttile.data.dao.PromptWordDao
import com.blogspot.yotsudev.prompttile.data.dao.SavedPromptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "prompt_tile.db",
    )
        /**
         * 開発中はスキーマ変更のたびにDBを破棄して再作成する。
         * MIGRATION_1_2 も不要になるため削除済み。
         *
         * ⚠️ リリース前には必ず以下の手順で正式マイグレーションに戻すこと：
         * 1. この行を削除
         * 2. version を上げる
         * 3. Migration オブジェクトを追加して addMigrations() で登録
         */
        .fallbackToDestructiveMigration()
        .addCallback(AppDatabase.seedCallback(context))  // ← context を渡す
        .build()

    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun providePromptWordDao(db: AppDatabase): PromptWordDao = db.promptWordDao()
    @Provides fun provideSavedPromptDao(db: AppDatabase): SavedPromptDao = db.savedPromptDao()
}