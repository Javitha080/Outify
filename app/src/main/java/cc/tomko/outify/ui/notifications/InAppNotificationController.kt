package cc.tomko.outify.ui.notifications

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicLong

@Immutable
data class NotificationSpec(
    val message: String,
    val icon: (@Composable (() -> Unit))? = null,
    val durationMillis: Long = 1500L,
    val placement: NotificationPlacement = NotificationPlacement.Bottom,
    val backgroundColor: Color? = null,
    val contentColor: Color? = null,
    val dismissOnTap: Boolean = true,
    val allowSwipeToDismiss: Boolean = true,
    val id: Long = IdGenerator.nextId(),
    val actions: (@Composable RowScope.() -> Unit)? = null
)

enum class NotificationPlacement { Top, Bottom }

private object IdGenerator {
    private val counter = AtomicLong(1)
    fun nextId() = counter.getAndIncrement()
}

object InAppNotificationController {
    private val _events = MutableSharedFlow<NotificationSpec>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    fun show(spec: NotificationSpec): Boolean = _events.tryEmit(spec)

    fun show(
        message: String,
        icon: (@Composable (() -> Unit))? = null,
        durationMillis: Long = 1600L
    ) =
        show(NotificationSpec(message = message, icon = icon, durationMillis = durationMillis))
}
