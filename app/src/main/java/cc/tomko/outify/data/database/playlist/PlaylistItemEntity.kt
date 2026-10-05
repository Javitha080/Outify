package cc.tomko.outify.data.database.playlist

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import cc.tomko.outify.data.database.PlaylistEntity

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistItemEntity(
    val playlistId: String,
    val position: Int,
    val trackUri: String,
    val addedBy: String,
    val timestamp: Long,
    val seenAt: Long,
    val isPublic: Boolean,
)