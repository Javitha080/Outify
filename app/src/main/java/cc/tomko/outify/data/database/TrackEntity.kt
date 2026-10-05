package cc.tomko.outify.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A database track entity.
 * Used for caching known track data.
 */
@Entity(
    tableName = "tracks",
    indices = [
        Index("id"),
        Index("albumId"),
        Index("lastAccessed"),
    ]
)
data class TrackEntity(
    @PrimaryKey val id: String,
    val trackUri: String, // Spotify URI
    val name: String,

    val albumId: String?,
    val albumName: String?,

    val durationMs: Long,
    val explicit: Boolean,
    val popularity: Int,

    // Cache control
    var isLibraryItem: Boolean,
    val lastAccessed: Long,
    val lastUpdated: Long,
)