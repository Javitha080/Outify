package cc.tomko.outify.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LyricsResponse(
    val lyrics: Lyrics
)

@Serializable
data class Lyrics(
    val syncType: String,
    val lines: List<LyricLine>
)

@Serializable
data class LyricLine(
    val startTimeMs: String,
    val words: String,
    val endTimeMs: String? = null
)

data class SyncedLyric(
    val timeMs: Long,
    val text: String
)