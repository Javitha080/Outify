package cc.tomko.outify.playback.model

import cc.tomko.outify.core.model.Track

/**
 * Holds current playback state
 */
data class PlaybackState(
    val state: PlayState = PlayState.IDLE,
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = 0,
    val position: PositionInfo = PositionInfo.EMPTY,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val shuffleEnabled: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val isActiveDevice: Boolean = false,
    val volume: Int = 65_535,
)
