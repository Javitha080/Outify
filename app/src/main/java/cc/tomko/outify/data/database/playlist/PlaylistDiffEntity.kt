package cc.tomko.outify.data.database.playlist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_diffs")
data class PlaylistDiffEntity(
    @PrimaryKey val playlistUri: String,
    @ColumnInfo(name = "diff_blob") val diffBlob: ByteArray,
    @ColumnInfo(name = "server_revision") val serverRevision: String,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long = System.currentTimeMillis()
)
