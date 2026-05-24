package com.blogspot.yotsudev.prompttile.di

import android.content.Context
import androidx.room.Room
import com.blogspot.yotsudev.prompttile.data.db.AppDatabase
import com.blogspot.yotsudev.prompttile.data.dao.CategoryDao
import com.blogspot.yotsudev.prompttile.data.dao.PromptWordDao
import com.blogspot.yotsudev.prompttile.data.dao.SavedPromptDao
import com.blogspot.yotsudev.prompttile.data.dao.ToppingDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "prompt_tile.db")
            .fallbackToDestructiveMigration()
            .addCallback(AppDatabase.seedCallback(context))
            .build()

    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun providePromptWordDao(db: AppDatabase): PromptWordDao = db.promptWordDao()
    @Provides fun provideSavedPromptDao(db: AppDatabase): SavedPromptDao = db.savedPromptDao()
    @Provides fun provideToppingDao(db: AppDatabase): ToppingDao = db.toppingDao() // 追加
}