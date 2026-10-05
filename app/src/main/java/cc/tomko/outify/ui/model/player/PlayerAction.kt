package cc.tomko.outify.ui.model.player

sealed interface PlayerAction {
    data object PlayPause : PlayerAction
    data object Next : PlayerAction
    data object Previous : PlayerAction
    data object ShuffleToggle : PlayerAction
    data object RepeatToggle : PlayerAction
    data class SeekTo(val position: Long) : PlayerAction
}