package cc.tomko.outify.data.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "track_artists",
    primaryKeys = ["trackId", "artistId"],
    indices = [Index("artistId")]
)
data class TrackArtistEntity(
    val trackId: String,
    val artistId: String,
    val position: Int
)