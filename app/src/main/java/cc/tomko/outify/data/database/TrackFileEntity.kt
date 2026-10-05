package cc.tomko.outify.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import cc.tomko.outify.core.model.FileType

@Entity(
    tableName = "track_files",
    primaryKeys = ["trackId", "type"],
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId")]
)
data class TrackFileEntity(
    val trackId: String,
    val type: FileType,
    val fileId: String
)
