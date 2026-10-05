package cc.tomko.outify.data.database.playlist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class PlaylistOperationDto {
    @Serializable
    @SerialName("add")
    data class Add(
        @SerialName("from_index") val fromIndex: Int,
        val items: List<PlaylistItemDto> = emptyList(),
        @SerialName("add_last") val addLast: Boolean = false,
        @SerialName("add_first") val addFirst: Boolean = false
    ) : PlaylistOperationDto()

    @Serializable
    @SerialName("rem")
    data class Remove(
        @SerialName("from_index") val fromIndex: Int,
        val length: Int = 0,
        val items: List<PlaylistItemDto> = emptyList(),
        @SerialName("has_items_as_key") val hasItemsAsKey: Boolean = false
    ) : PlaylistOperationDto()

    @kotlinx.serialization.Serializable
    @SerialName("mov")
    data class Move(
        @SerialName("from_index") val fromIndex: Int,
        val length: Int,
        @SerialName("to_index") val toIndex: Int
    ) : PlaylistOperationDto()

    @Serializable
    @SerialName("update_item_attributes")
    data class UpdateItemAttributes(
        // If `position`/`length` are present, update a range; if `items` present, match by identity
        @SerialName("position") val position: Int? = null,
        @SerialName("length") val length: Int? = null,
        val items: List<PlaylistItemDto> = emptyList(),
        @SerialName("added_by") val addedBy: String? = null,
        val timestamp: Long? = null,
        @SerialName("seen_at") val seenAt: Long? = null,
        @SerialName("is_public") val isPublic: Boolean? = null
    ) : PlaylistOperationDto()

    @Serializable
    @SerialName("update_list_attributes")
    data class UpdateListAttributes(
        // List-level changes — handled at playlist metadata level, not by item applier
        val name: String? = null,
        val description: String? = null,
        @SerialName("is_deleted_by_owner") val isDeletedByOwner: Boolean? = null
    ) : PlaylistOperationDto()
}

@Serializable
data class PlaylistDiffDto(
    @SerialName("from_revision") val fromRevision: String,
    @SerialName("to_revision") val toRevision: String,
    val operations: List<PlaylistOperationDto>
)

@Serializable
data class PlaylistItemDto(
    @SerialName("track_uri") val trackUri: String,
    @SerialName("added_by") val addedBy: String? = null,
    val timestamp: Long? = null,
    @SerialName("seen_at") val seenAt: Long? = null,
    @SerialName("is_public") val isPublic: Boolean? = null
)