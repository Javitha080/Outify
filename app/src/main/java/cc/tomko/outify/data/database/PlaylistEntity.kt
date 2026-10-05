package cc.tomko.outify.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val uri: String,
    val ownerUsername: String,
    val revision: String,
    val name: String,
    val description: String,
    val pictureId: String,
    val isCollaborative: Boolean,
    val isDeletedByOwner: Boolean,
    val timestamp: Long,
)

fun PlaylistEntity.canModify(username: String): Boolean =
    this.isCollaborative || this.ownerUsername == username