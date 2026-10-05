package cc.tomko.outify.data.database

import androidx.room.Embedded
import androidx.room.Relation

data class TrackWithFiles(
    @Embedded val track: TrackEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "trackId"
    )
    val files: List<TrackFileEntity>
)
