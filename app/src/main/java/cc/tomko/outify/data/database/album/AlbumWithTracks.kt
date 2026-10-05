package cc.tomko.outify.data.database.album

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import cc.tomko.outify.data.database.AlbumEntity
import cc.tomko.outify.data.database.TrackEntity

data class AlbumWithTracks(
    @Embedded val album: AlbumEntity,

    @Relation(
        parentColumn = "albumId",
        entityColumn = "id",
        associateBy = Junction(
            value = AlbumTrackCrossRef::class,
            parentColumn = "albumId",
            entityColumn = "trackId"
        )
    )
    val tracks: List<TrackEntity>
)
