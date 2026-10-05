package cc.tomko.outify.data.database.album

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "album_tracks",
    primaryKeys = ["albumId", "trackId"],
    indices = [Index("albumId"), Index("trackId")]
)
data class AlbumTrackCrossRef(
    val albumId: String,
    val trackId: String,
    val position: Int
)
