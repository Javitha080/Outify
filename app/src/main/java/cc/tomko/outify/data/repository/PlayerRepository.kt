package cc.tomko.outify.data.repository

import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.model.LyricsResponse
import cc.tomko.outify.core.model.SyncedLyric
import cc.tomko.outify.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import javax.inject.Inject

class PlayerRepository @Inject constructor(
    private val spClient: SpClient,
    private val json: Json,
) {
    suspend fun getLyrics(track: Track?, timeoutMs: Long = 2000L): List<SyncedLyric> =
        withContext(Dispatchers.IO) {
            val id = track?.id ?: return@withContext emptyList()

            val raw: String = try {
                withTimeout(timeoutMs) {
                    spClient.getLyrics(id) ?: ""
                }
            } catch (e: Exception) {
                android.util.Log.w("PlayerRepository", "Lyrics fetch failed", e)
                return@withContext emptyList()
            }

            return@withContext try {
                if (raw.trimStart().startsWith("{\"error\"")) return@withContext emptyList()
                val response: LyricsResponse = json.decodeFromString(raw)
                response.lyrics.lines
                    .filter { it.words.isNotBlank() }
                    .map {
                        SyncedLyric(
                            timeMs = it.startTimeMs.toLong(),
                            text = it.words
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.w("PlayerRepository", "Lyrics decode failed", e)
                emptyList()
            }
        }
}
