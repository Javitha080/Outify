package cc.tomko.outify.ui.screens.library.track

import cc.tomko.outify.core.model.SyncedLyric
import cc.tomko.outify.core.model.Track

data class TrackUiState(
    val isLoading: Boolean = true,
    val track: Track? = null,
    val lyrics: List<SyncedLyric> = emptyList(),
    val error: String? = null,
)
