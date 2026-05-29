package com.blogspot.yotsudev.prompttile.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parent_categories")
data class ParentCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nameJa: String,
    val nameEn: String,
    val sortOrder: Int = 0,
    val isNegative: Boolean = false,
)
