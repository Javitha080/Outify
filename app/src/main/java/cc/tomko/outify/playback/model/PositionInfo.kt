package cc.tomko.outify.playback.model

import kotlin.time.Duration

data class PositionInfo(
    /**
     * The current position.
     */
    val active: Duration,

    /**
     * The maximum position
     */
    val duration: Duration,

    /**
     * Time of last sync
     */
    val lastSync: Long
) {
    companion object {
        val EMPTY = PositionInfo(Duration.ZERO, Duration.ZERO, 0L)
    }
}
