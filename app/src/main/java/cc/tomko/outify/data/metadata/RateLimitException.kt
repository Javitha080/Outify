package cc.tomko.outify.data.metadata

class RateLimitException(
    message: String,
    val retryAfterSeconds: Long? = null
) : Exception(message)

