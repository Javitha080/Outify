package cc.tomko.outify.core

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthCallbackServerManager @Inject constructor() {
    @Volatile
    private var server: AuthCallbackServer? = null

    fun start(onCodeReceived: (code: String, state: String?) -> Unit) {
        synchronized(this) {
            val existing = server
            if (existing?.isAlive == true) {
                existing.onCodeReceived = onCodeReceived
                return
            }
            try { existing?.stop() } catch (_: Exception) { }
            server = AuthCallbackServer(onCodeReceived = onCodeReceived).apply {
                try { start() } catch (e: Exception) {
                    android.util.Log.w("AuthCallbackServerManager", "Loopback start failed", e)
                    server = null
                }
            }
        }
    }

    fun stop() {
        synchronized(this) {
            try { server?.stop() } catch (_: Exception) { }
            server = null
        }
    }
}
