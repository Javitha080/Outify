package cc.tomko.outify.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Holds the User profile data
 */
@Serializable
data class Profile(
    val uri: String,
    val name: String? = null,
    @SerialName("is_following")
    val isFollowing: Boolean = false,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("followers_count")
    val followersCount: Int?,
    @SerialName("following_count")
    val followingCount: Int?,
    val color: Int,
    @SerialName("allow_follows")
    val allowFollows: Boolean?,

    @SerialName("total_playlist_count")
    val totalPlaylistCount: Int? = null,
    @SerialName("public_playlists")
    val publicPlaylists: List<ProfilePlaylist> = emptyList()
)

@Serializable
data class ProfilePlaylist(
    val uri: String,
    val name: String,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("followers_count")
    val followersCount: Int,
    @SerialName("owner_uri")
    val ownerUri: String,
    @SerialName("is_following")
    val isFollowing: Boolean,
)