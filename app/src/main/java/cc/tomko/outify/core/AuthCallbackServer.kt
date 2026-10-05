package cc.tomko.outify.core

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

class AuthCallbackServer(
    port: Int = 5588,
    private val packageName: String = "cc.tomko.outify",
    @Volatile var onCodeReceived: (code: String, state: String?) -> Unit
) : NanoHTTPD(port) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val stopDelayMs = 350L
    private val consumed = AtomicBoolean(false)

    override fun serve(session: IHTTPSession): Response {
        if (!isLoopback(session.remoteIpAddress)) {
            Log.w("AuthCallbackServer", "Rejected non-loopback caller")
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "text/plain", "Forbidden")
        }
        return try {
            when (session.uri) {
                "/login" -> handleCallback(
                    session,
                    success = "auth_complete",
                    failure = "auth_failed"
                )

                "/account/login" -> handleCallback(
                    session,
                    success = "account_auth_complete",
                    failure = "account_auth_failed"
                )

                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
            }
        } catch (e: SocketException) {
            Log.w("AuthCallbackServer", "Socket closed while responding")
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "")
        } catch (t: Throwable) {
            Log.e("AuthCallbackServer", "Callback failed", t)
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error")
        }
    }

    private fun isLoopback(remoteIp: String?): Boolean {
        return remoteIp == "127.0.0.1" || remoteIp == "::1" || remoteIp == "0:0:0:0:0:0:0:1"
    }

    private fun isValidCode(code: String): Boolean {
        if (code.length < 4 || code.length > 512) return false
        return code.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }
    }

    private fun handleCallback(
        session: IHTTPSession,
        success: String,
        failure: String
    ): Response {
        val code = session.parameters["code"]?.firstOrNull()
        val state = session.parameters["state"]?.firstOrNull()

        if (code == null || !isValidCode(code)) {
            return redirect("intent://$failure#Intent;scheme=outify;package=$packageName;end")
        }
        if (!consumed.compareAndSet(false, true)) {
            return newFixedLengthResponse(Response.Status.GONE, "text/plain", "Already used")
        }

        scope.launch {
            delay(stopDelayMs)
            try {
                onCodeReceived(code, state)
            } finally {
                try {
                    stop()
                } catch (_: Throwable) {
                }
            }
        }

        return redirect("intent://$success#Intent;scheme=outify;package=$packageName;end")
    }

    private fun redirect(location: String): Response =
        newFixedLengthResponse(Response.Status.REDIRECT, "text/plain", "").apply {
            addHeader("Location", location)
            addHeader("Connection", "close")
        }
}