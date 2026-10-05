package cc.tomko.outify.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "liked_items")
data class LikedItemsEntity(
    @PrimaryKey
    val uri: String,
    val type: String, // "playlist", "album", "artist"
    val addedAt: Long = System.currentTimeMillis()
)