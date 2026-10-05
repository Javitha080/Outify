package cc.tomko.outify

import android.os.Bundle
import androidx.media3.session.SessionCommand

object MediaSessionConstants {
    const val ACTION_TOGGLE_LIKE = "toggle_like"
    const val ACTION_TOGGLE_START_RADIO = "toggle_start_radio"
    const val ACTION_TOGGLE_SHUFFLE = "toggle_shuffle"
    const val ACTION_TOGGLE_REPEAT_MODE = "toggle_repeat_mode"

    val CommandToggleLike = SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY)
    val CommandToggleStartRadio = SessionCommand(ACTION_TOGGLE_START_RADIO, Bundle.EMPTY)
    val CommandToggleShuffle = SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY)
    val CommandToggleRepeatMode = SessionCommand(ACTION_TOGGLE_REPEAT_MODE, Bundle.EMPTY)
}