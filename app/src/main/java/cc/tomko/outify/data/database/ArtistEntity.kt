package cc.tomko.outify.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import cc.tomko.outify.core.model.Artist

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val artistId: String,
    val uri: String,
    val name: String,
    val imageUrl: String?,
    val popularity: Int,

    val lastUpdated: Long,
)

fun ArtistEntity.toDomain(): Artist {
    return Artist(
        id = artistId,
        uri = uri,
        name = name,
        popularity = popularity,
        portraits = emptyList(),
        tracks = emptyList(),
    )
}