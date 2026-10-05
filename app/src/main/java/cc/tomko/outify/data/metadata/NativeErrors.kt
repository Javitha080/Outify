package cc.tomko.outify.data.metadata

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class NativeError {
    abstract val message: String
    abstract val type: String

    data class RateLimited(
        override val message: String,
        val retryAfterSeconds: Long? = null
    ) : NativeError() {
        override val type: String = "rate_limit"
    }

    data class AuthenticationError(
        override val message: String
    ) : NativeError() {
        override val type: String = "authentication_error"
    }

    data class ServiceUnavailable(
        override val message: String
    ) : NativeError() {
        override val type: String = "service_unavailable"
    }

    data class Unknown(
        override val message: String,
        override val type: String = "unknown"
    ) : NativeError()

    companion object {
        fun fromJson(type: String, message: String, retryAfterSeconds: Long? = null): NativeError {
            return when (type) {
                "rate_limit" -> RateLimited(message, retryAfterSeconds)
                "authentication_error", "authentication" -> AuthenticationError(message)
                "service_unavailable", "unavailable" -> ServiceUnavailable(message)
                "unknown" -> {
                    val lowerMessage = message.lowercase()
                    when {
                        lowerMessage.contains("token") && lowerMessage.contains("credentials") ->
                            AuthenticationError(message)

                        lowerMessage.contains("service unavailable") ->
                            ServiceUnavailable(message)

                        else -> Unknown(message, type)
                    }
                }

                else -> Unknown(message, type)
            }
        }
    }
}

object NativeErrorHandler {
    private const val TAG = "NativeErrorHandler"
    private val scope = CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob())

    interface ErrorCallback {
        fun onError(error: NativeError)
    }

    private val lock = Any()
    private var callbacks = mutableListOf<ErrorCallback>()

    fun registerCallback(callback: ErrorCallback) {
        synchronized(lock) { callbacks.add(callback) }
    }

    fun unregisterCallback(callback: ErrorCallback) {
        synchronized(lock) { callbacks.remove(callback) }
    }

    fun handleError(error: NativeError, context: String = "") {
        val fullMessage = if (context.isNotEmpty()) "$context: ${error.message.take(300)}" else error.message.take(300)

        Log.e(TAG, "Native error [${error.type}]: $fullMessage")

        val current = synchronized(lock) { callbacks.toList() }
        scope.launch {
            current.forEach {
                runCatching { it.onError(error) }
            }
        }

        when (error) {
            is NativeError.AuthenticationError -> handleAuthError(error)
            is NativeError.RateLimited -> handleRateLimit(error)
            is NativeError.ServiceUnavailable -> handleServiceUnavailable(error)
            is NativeError.Unknown -> handleUnknownError(error)
        }
    }

    fun handleErrorJson(jsonString: String, context: String = ""): NativeError? {
        if (!jsonString.trimStart().startsWith("{")) return null
        return try {
            val obj = org.json.JSONObject(jsonString)
            if (obj.has("error")) {
                val err = obj.getJSONObject("error")
                val type = err.optString("type", "unknown")
                val message = err.optString("message", "Unknown error").take(500)
                val retryAfter =
                    if (err.has("retry_after_seconds")) err.optLong("retry_after_seconds") else null

                val error = NativeError.fromJson(type, message, retryAfter)
                handleError(error, context)
                error
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse error JSON", e)
            null
        }
    }

    private fun handleAuthError(error: NativeError.AuthenticationError) {
        Log.w(TAG, "Authentication error: ${error.message}")
    }

    private fun handleRateLimit(error: NativeError.RateLimited) {
        Log.w(TAG, "Rate limited: ${error.message}, retry after ${error.retryAfterSeconds}s")
    }

    private fun handleServiceUnavailable(error: NativeError.ServiceUnavailable) {
        Log.w(TAG, "Service unavailable: ${error.message}")
    }

    private fun handleUnknownError(error: NativeError.Unknown) {
        Log.w(TAG, "Unknown native error: ${error.message}")
    }
}
