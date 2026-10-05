package cc.tomko.outify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cc.tomko.outify.data.database.LikedItemsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LikedItemsDao {
    @Query("SELECT uri FROM liked_items WHERE type = :type")
    suspend fun getUrisByType(type: String): List<String>

    @Query("SELECT uri FROM liked_items WHERE type = :type")
    fun observeUrisByType(type: String): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_items WHERE uri = :uri)")
    suspend fun contains(uri: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM liked_items WHERE uri = :uri)")
    fun observeContains(uri: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LikedItemsEntity)

    @Query("DELETE FROM liked_items WHERE uri = :uri")
    suspend fun delete(uri: String)

    @Query("DELETE FROM liked_items")
    suspend fun clearAll()
}