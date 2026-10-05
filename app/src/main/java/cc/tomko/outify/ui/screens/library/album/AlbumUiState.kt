package cc.tomko.outify.ui.screens.library.album

import cc.tomko.outify.core.model.Album
import cc.tomko.outify.core.model.Track
import kotlinx.serialization.Serializable

@Serializable
data class AlbumUiState(
    val isLoading: Boolean = true,
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isPlayable: Boolean = true,
    val error: String? = null
)

