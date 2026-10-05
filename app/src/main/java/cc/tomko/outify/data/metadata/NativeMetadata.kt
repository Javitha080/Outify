package cc.tomko.outify.data.metadata

import android.util.Log
import kotlinx.coroutines.delay
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class NativeResult(
    val metadata: JSONObject? = null,
    val error: cc.tomko.outify.data.metadata.NativeError? = null
)

@Singleton
class NativeMetadata @Inject constructor() {
    companion object {
        private const val TAG = "NativeMetadata"
    }

    fun fetchMetadata(uri: String): JSONObject {
        val result = getNativeResult(uri)

        result.error?.let { err ->
            NativeErrorHandler.handleError(err, "fetchMetadata:$uri")

            when (err) {
                is cc.tomko.outify.data.metadata.NativeError.RateLimited ->
                    throw RateLimitException(err.message, err.retryAfterSeconds)

                else -> throw NativeOperationException(err.message, err)
            }
        }

        return result.metadata ?: throw Exception("Metadata is null")
    }

    private fun getNativeResult(uri: String): NativeResult {
        val result = try {
            getNativeMetadata(uri)
        } catch (e: Exception) {
            Log.w(TAG, "Native call failed for $uri", e)
            return NativeResult(
                metadata = null,
                error = cc.tomko.outify.data.metadata.NativeError.Unknown("Native call failed")
            )
        } ?: return NativeResult(
            metadata = null,
            error = cc.tomko.outify.data.metadata.NativeError.Unknown("Empty response")
        )

        if (result.trimStart().startsWith("{")) {
            try {
                val obj = JSONObject(result)
                if (obj.has("error")) {
                    val err = obj.getJSONObject("error")
                    val type = err.optString("type", "unknown")
                    val message = err.optString("message", "Unknown error")
                    val retryAfter =
                        if (err.isNull("retry_after_seconds")) null else err.optLong("retry_after_seconds")

                    val nativeError =
                        cc.tomko.outify.data.metadata.NativeError.fromJson(type, message, retryAfter)
                    return NativeResult(metadata = null, error = nativeError)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error envelope parse failed", e)
            }
        }

        if (result.trimStart().startsWith("{")) {
            try {
                return NativeResult(metadata = JSONObject(result), error = null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse metadata", e)
            }
        }

        return NativeResult(
            metadata = null,
            error = cc.tomko.outify.data.metadata.NativeError.Unknown("Invalid response")
        )
    }

    suspend fun <T> retryOnRateLimit(
        maxAttempts: Int = 5,
        baseDelayMs: Long = 1000L,
        block: suspend () -> T
    ): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: RateLimitException) {
                attempt++
                if (attempt >= maxAttempts) throw e

                val backoffMs =
                    e.retryAfterSeconds?.times(1000L) ?: (baseDelayMs * (1L shl (attempt - 1)))
                val jitter = Random.nextLong(0, (backoffMs / 3).coerceAtLeast(1L))
                val delayMs = backoffMs + jitter

                delay(delayMs)
            }
        }
    }

    external fun getNativeMetadata(uri: String): String?
}

class NativeOperationException(
    message: String,
    val error: cc.tomko.outify.data.metadata.NativeError
) : Exception(message)