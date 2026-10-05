package cc.tomko.outify.data.database.track

import androidx.room.Entity
import androidx.room.Index

@Entity(
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index("trackId")]
)
data class PlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val position: Int,
)
