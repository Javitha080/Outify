package cc.tomko.outify.core

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles the librespot session
 */
@Singleton
class Session @Inject constructor() {
    external fun initializeSession(callback: SessionCallback)

    external fun shutdown(): Boolean

    external fun unregisterSessionCallback()
}

interface SessionCallback {
    /**
     * Called when the session gets initialized
     */
    fun onInitialized()

    /**
     * Called when the session shutdowns
     */
    fun onShutdown()

    /**
     * Called when the session auto restarts from shutdown
     */
    fun onAutoRestart()
}