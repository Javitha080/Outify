package cc.tomko.outify.utils

fun canonicalIdFromUri(uriOrId: String): String =
    uriOrId.substringAfterLast(":").trim()

fun isValidSpotifyPlaylistUri(uri: String): Boolean {
    return uri.startsWith("spotify:playlist:") &&
            uri.length > "spotify:playlist:".length && uri.length < "spotify:playlist:".length + 22
}
