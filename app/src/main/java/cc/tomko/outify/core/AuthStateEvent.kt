package cc.tomko.outify.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class AuthStateEvent {
    data object AccountLoggedIn : AuthStateEvent()
    data object AccountLoggedOut : AuthStateEvent()
    data object PlaybackLoggedIn : AuthStateEvent()
    data object PlaybackLoggedOut : AuthStateEvent()
}

object AuthStateEventBus {
    private val _events = MutableSharedFlow<AuthStateEvent>(replay = 0)
    val events: SharedFlow<AuthStateEvent> = _events.asSharedFlow()

    suspend fun emitAccountLoggedIn() {
        _events.emit(AuthStateEvent.AccountLoggedIn)
    }

    suspend fun emitAccountLoggedOut() {
        _events.emit(AuthStateEvent.AccountLoggedOut)
    }

    suspend fun emitPlaybackLoggedIn() {
        _events.emit(AuthStateEvent.PlaybackLoggedIn)
    }

    suspend fun emitPlaybackLoggedOut() {
        _events.emit(AuthStateEvent.PlaybackLoggedOut)
    }

    fun tryEmitAccountLoggedIn() {
        _events.tryEmit(AuthStateEvent.AccountLoggedIn)
    }

    fun tryEmitAccountLoggedOut() {
        _events.tryEmit(AuthStateEvent.AccountLoggedOut)
    }

    fun tryEmitPlaybackLoggedIn() {
        _events.tryEmit(AuthStateEvent.PlaybackLoggedIn)
    }

    fun tryEmitPlaybackLoggedOut() {
        _events.tryEmit(AuthStateEvent.PlaybackLoggedOut)
    }
}