package com.blogspot.yotsudev.prompttile.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.blogspot.yotsudev.prompttile.data.entity.ToppingGroupEntity
import com.blogspot.yotsudev.prompttile.data.entity.ToppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToppingDao : BaseDao<ToppingGroupEntity> {

    /** グループIDに紐づくアイテム一覧を sortOrder 順で取得（単発・suspend）*/
    @Query("SELECT * FROM topping_items WHERE groupId = :groupId ORDER BY sortOrder ASC")
    suspend fun getItemsByGroup(groupId: Long): List<ToppingItemEntity>

    /** グループIDに紐づくアイテム一覧をリアクティブに監視（Flow版）*/
    @Query("SELECT * FROM topping_items WHERE groupId = :groupId ORDER BY sortOrder ASC")
    fun observeItemsByGroup(groupId: Long): Flow<List<ToppingItemEntity>>

    @Query("SELECT * FROM topping_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): ToppingGroupEntity?

    /** アイテムの挿入（BaseDao を ToppingItemEntity に使えないため個別定義）*/
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: ToppingItemEntity): Long

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<ToppingItemEntity>): List<Long>
}