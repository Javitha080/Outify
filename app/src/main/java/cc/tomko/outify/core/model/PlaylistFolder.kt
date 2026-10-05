package cc.tomko.outify.core.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistFolder(
    val id: String,
    val name: String,
    val color: Long,
    val playlistIds: List<String> = emptyList(),
)

fun PlaylistFolder.toColor(): Color = Color(color.toInt())
