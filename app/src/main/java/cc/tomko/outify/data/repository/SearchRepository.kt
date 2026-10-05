package cc.tomko.outify.data.repository

import cc.tomko.outify.core.SpClient
import cc.tomko.outify.ui.model.search.SearchResult
import cc.tomko.outify.ui.model.search.SearchResultType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SearchRepository @Inject constructor(
    private val spClient: SpClient
) {
    val json = Json { ignoreUnknownKeys = true }

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val uris = spClient.search(query, "track,artist,album,playlist")

        uris.mapNotNull { uri ->
            val type = when {
                uri.startsWith("spotify:track:") -> SearchResultType.TRACK
                uri.startsWith("spotify:artist:") -> SearchResultType.ARTIST
                uri.startsWith("spotify:album:") -> SearchResultType.ALBUM
                uri.startsWith("spotify:playlist:") -> SearchResultType.PLAYLIST
                uri.startsWith("spotify:show:") -> SearchResultType.SHOW
                uri.startsWith("spotify:episode:") -> SearchResultType.EPISODE
                else -> null
            }

            type?.let { SearchResult(uri, it) }
        }
    }

    suspend fun searchByType(query: String, type: String): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val uris = spClient.search(query, type)
            uris.mapNotNull { uri ->
                val resultType = when {
                    uri.startsWith("spotify:track:") -> SearchResultType.TRACK
                    uri.startsWith("spotify:artist:") -> SearchResultType.ARTIST
                    uri.startsWith("spotify:album:") -> SearchResultType.ALBUM
                    uri.startsWith("spotify:playlist:") -> SearchResultType.PLAYLIST
                    uri.startsWith("spotify:show:") -> SearchResultType.SHOW
                    uri.startsWith("spotify:episode:") -> SearchResultType.EPISODE
                    else -> null
                }
                resultType?.let { SearchResult(uri, it) }
            }
        }
}