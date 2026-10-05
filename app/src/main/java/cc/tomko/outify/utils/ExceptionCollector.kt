package cc.tomko.outify.utils

import javax.inject.Inject
import javax.inject.Singleton

data class CapturedException(
    val message: String,
    val threadName: String,
    val stackTrace: String,
    val timestamp: String,
)

@Singleton
class ExceptionCollector @Inject constructor() {
    private val lock = Any()
    private val _exceptions = mutableListOf<CapturedException>()
    private val maxSize = 50

    val exceptions: List<CapturedException>
        get() = synchronized(lock) { _exceptions.toList() }

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            capture(thread.name, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun capture(threadName: String, throwable: Throwable) {
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()

        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        synchronized(lock) {
            if (_exceptions.size >= maxSize) _exceptions.removeAt(0)
            _exceptions.add(
                CapturedException(
                    message = throwable.message ?: throwable.javaClass.simpleName,
                    threadName = threadName,
                    stackTrace = sw.toString().take(4000),
                    timestamp = timestamp,
                )
            )
        }
    }
}
