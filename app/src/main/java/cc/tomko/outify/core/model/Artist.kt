package cc.tomko.outify.core.model

import kotlinx.serialization.Serializable

/**
 * Contains information about singular Artist.
 * Data sourced from JSON from FFI.
 */
@Serializable
data class Artist(
    val id: String,
    val uri: String,
    val name: String,
    val popularity: Int,
    val portraits: List<String> = emptyList(),
    val tracks: List<String> = emptyList(), // Just track uris
    val covers: List<Cover> = emptyList(),
    val albums: List<String> = emptyList(), // Just album uris
    val singles: List<String> = emptyList(), // Just album uris
)

fun Artist.getCover(size: CoverSize): Cover? {
    return covers.firstOrNull { it.size == size.asInt() }
}

fun Artist.toSpotifyUri(): SpotifyUri =
    SpotifyUri.Artist(id)

fun Artist.toOutifyUri(): OutifyUri =
    OutifyUri.fromUriString(uri)