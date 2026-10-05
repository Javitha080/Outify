package cc.tomko.outify.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentUserProfile(
    val country: String,
    @SerialName("display_name")
    val displayName: String,
    val email: String,
    @SerialName("explicit_content")
    val explicitContent: ExplicitContent,
    @SerialName("external_urls")
    val externalUrls: ExternalUrls,
    val followers: Followers,
    val href: String,
    val id: String,
    val images: List<Image>,
    val product: String,
    @SerialName("type")
    val userType: String,
    val uri: String
)

@Serializable
data class ExplicitContent(
    @SerialName("filter_enabled")
    val filterEnabled: Boolean,
    @SerialName("filter_locked")
    val filterLocked: Boolean
)

@Serializable
data class ExternalUrls(
    val spotify: String
)

@Serializable
data class Followers(
    val href: String?,
    val total: Int
)

@Serializable
data class Image(
    val url: String,
    val height: Int,
    val width: Int
)