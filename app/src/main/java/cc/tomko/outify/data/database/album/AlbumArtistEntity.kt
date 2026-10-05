package cc.tomko.outify.data.database.album

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "album_artists",
    primaryKeys = ["albumId", "artistId"],
    indices = [Index("artistId")]
)
data class AlbumArtistEntity(
    val albumId: String,
    val artistId: String,
    val position: Int
)