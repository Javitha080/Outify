package cc.tomko.outify.core.model

import android.util.Base64
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.data.database.PlaylistEntity
import cc.tomko.outify.data.database.playlist.PlaylistItemDto
import cc.tomko.outify.data.database.playlist.PlaylistItemEntity
import cc.tomko.outify.data.metadata.Metadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Playlist(
    val id: String,
    val uri: String,

    @SerialName("owner_username")
    val ownerUsername: String,

    val revision: String,
    val length: Int, // Rust used i32
    val attributes: PlaylistAttributes,
    val contents: List<PlaylistItem>,
    val timestamp: Long,
    val diff: PlaylistDiff? = null
)

@Serializable
data class PlaylistAttributes(
    val name: String,
    val description: String,
    /**
     * Raw picture bytes as array of numbers to match Vec<u8> serialization
     */
    val picture: List<Int> = emptyList(),

    @SerialName("is_collaborative")
    val isCollaborative: Boolean,

    @SerialName("is_deleted_by_owner")
    val isDeletedByOwner: Boolean,

    /**
     * hex id of the picture
     */
    @SerialName("picture_id")
    val pictureId: String,
)

@Serializable
data class PlaylistItem(
    val id: String,
    val uri: String,
    val attributes: PlaylistItemAttributes
)

@Serializable
data class PlaylistItemAttributes(
    @SerialName("added_by")
    val addedBy: String,

    val timestamp: Long,

    @SerialName("seen_at")
    val seenAt: Long,

    @SerialName("is_public")
    val isPublic: Boolean,
)

/**
 * Minimal representations for diff and operations
 */
@Serializable
data class PlaylistDiff(
    @SerialName("from_revision")
    val fromRevision: String,
    @SerialName("to_revision")
    val toRevision: String,
    val operations: List<PlaylistOperation>
)

@Serializable
data class PlaylistOperation(
    val kind: String,

    // Only one of these will generally be present depending on kind
    val add: PlaylistOperationAdd? = null,
    val rem: PlaylistOperationRemove? = null,
    val mov: PlaylistOperationMove? = null,
    @SerialName("update_item_attributes")
    val updateItemAttributes: PlaylistUpdateItemAttributes? = null,
    @SerialName("update_list_attributes")
    val updateListAttributes: PlaylistUpdateListAttributes? = null
)

@Serializable
data class PlaylistOperationAdd(
    val from_index: Int,
    val items: List<PlaylistItem>,
    val add_last: Boolean,
    val add_first: Boolean
)

@Serializable
data class PlaylistOperationRemove(
    val from_index: Int,
    val length: Int,
    val items: List<PlaylistItem>,
    val has_items_as_key: Boolean
)

@Serializable
data class PlaylistOperationMove(
    val from_index: Int,
    val length: Int,
    val to_index: Int
)

@Serializable
data class PlaylistUpdateItemAttributes(
    // mirrors the Rust minimal update object
    val position: Int? = null,
    val length: Int? = null,
    val items: List<PlaylistItem> = emptyList(),
    val added_by: String? = null,
    val timestamp: Long? = null,
    @SerialName("seen_at")
    val seenAt: Long? = null,
    @SerialName("is_public")
    val isPublic: Boolean? = null,
    @SerialName("format_attributes")
    val formatAttributes: Map<String, String>? = null
)

@Serializable
data class PlaylistUpdateListAttributes(
    val name: String? = null,
    val description: String? = null,
    @SerialName("is_deleted_by_owner")
    val isDeletedByOwner: Boolean? = null,
    @SerialName("format_attributes")
    val formatAttributes: Map<String, String>? = null,
    @SerialName("picture_sizes")
    val pictureSizes: JsonElement? = null
)

fun List<Int>.toByteArray(): ByteArray = ByteArray(size) { i -> (this[i] and 0xFF).toByte() }

fun List<Int>.toBase64(): String =
    Base64.encodeToString(this.toByteArray(), Base64.NO_WRAP)

fun PlaylistItem.toDto(): PlaylistItemDto {
    return PlaylistItemDto(
        trackUri = uri
    )
}

suspend fun Playlist.getCover(metadata: Metadata, size: CoverSize = CoverSize.MEDIUM): String? {
    if (attributes.pictureId.isNotEmpty()) {
        return ALBUM_COVER_URL + attributes.pictureId
    }

    // Getting based on the first track
    val trackId: String = contents.firstOrNull()?.id ?: return null
    val cover = metadata.getAlbumCoverByTrackId(trackId, size) ?: return null

    return ALBUM_COVER_URL + cover.uri
}

fun Playlist.toEntity(): PlaylistEntity =
    PlaylistEntity(
        id = id,
        uri = uri,
        ownerUsername = ownerUsername,
        revision = revision,
        name = attributes.name,
        description = attributes.description,
        pictureId = attributes.pictureId,
        isCollaborative = attributes.isCollaborative,
        isDeletedByOwner = attributes.isDeletedByOwner,
        timestamp = timestamp,
    )

fun Playlist.toItemEntities(): List<PlaylistItemEntity> =
    contents.mapIndexed { index, item ->
        PlaylistItemEntity(
            playlistId = id,
            position = index,
            trackUri = item.uri,
            addedBy = item.attributes.addedBy,
            timestamp = item.attributes.timestamp,
            seenAt = item.attributes.seenAt,
            isPublic = item.attributes.isPublic,
        )
    }

fun Playlist.toSpotifyUri(): SpotifyUri =
    SpotifyUri.Playlist(id)

fun Playlist.toOutifyUri(): OutifyUri =
    OutifyUri.fromUriString(uri)

fun Playlist.canModify(username: String): Boolean =
    this.attributes.isCollaborative || this.ownerUsername == username