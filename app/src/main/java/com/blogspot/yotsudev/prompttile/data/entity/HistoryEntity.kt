package com.blogspot.yotsudev.prompttile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "copy_histories")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val positivePrompt: String,
    val negativePrompt: String,
    val timestamp: Long = System.currentTimeMillis()
)
