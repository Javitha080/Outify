package cc.tomko.outify.data.database.playlist

import androidx.room.Embedded
import androidx.room.Relation
import cc.tomko.outify.core.model.Playlist
import cc.tomko.outify.core.model.PlaylistAttributes
import cc.tomko.outify.core.model.PlaylistItem
import cc.tomko.outify.core.model.PlaylistItemAttributes
import cc.tomko.outify.data.database.PlaylistEntity

data class PlaylistWithItems(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlistId",
        entity = PlaylistItemEntity::class
    )
    val items: List<PlaylistItemEntity>
)

fun PlaylistWithItems.toDomainOrNull(): Playlist? = runCatching {
    this.toDomain()
}.getOrNull()

fun PlaylistWithItems.toDomain(): Playlist {
    val orderedItems = items.sortedBy { it.position }

    return Playlist(
        id = playlist.id,
        uri = playlist.uri,
        ownerUsername = playlist.ownerUsername,
        revision = playlist.revision,
        length = orderedItems.size,
        attributes = PlaylistAttributes(
            name = playlist.name,
            description = playlist.description,
            pictureId = playlist.pictureId,
            picture = emptyList(),
            isCollaborative = playlist.isCollaborative,
            isDeletedByOwner = playlist.isDeletedByOwner,
        ),
        contents = orderedItems.map { item ->
            PlaylistItem(
                id = item.trackUri.substringAfterLast(":"),
                uri = item.trackUri,
                attributes = PlaylistItemAttributes(
                    addedBy = item.addedBy,
                    timestamp = item.timestamp,
                    seenAt = item.seenAt,
                    isPublic = item.isPublic
                ),
            )
        },

        diff = null,
        timestamp = System.currentTimeMillis(),
    )
}

fun PlaylistWithItems.canModify(username: String): Boolean =
    playlist.isCollaborative || playlist.ownerUsername == username
