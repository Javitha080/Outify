package cc.tomko.outify.ui.notifications

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun InAppNotificationHost(
    modifier: Modifier = Modifier,
    hostPaddingBottom: Dp = 0.dp,
    maxWidthFraction: Float = 0.92f,
    contentAlignmentBottom: Alignment = Alignment.BottomCenter,
    contentAlignmentTop: Alignment = Alignment.TopCenter,
) {
    val scope = rememberCoroutineScope()
    val events = InAppNotificationController.events

    var current by remember { mutableStateOf<NotificationSpec?>(null) }
    var dismissJob by remember { mutableStateOf<Job?>(null) }

    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.88f) }

    var bannerHeightPx by remember { mutableIntStateOf(0) }

    fun exitOffset(placement: NotificationPlacement, height: Float): Float =
        if (placement == NotificationPlacement.Bottom) height else -height

    LaunchedEffect(events) {
        events.collectLatest { spec ->
            if (current != null) {
                dismissJob?.cancel()
                launch { alpha.animateTo(0f, tween(120)) }
                launch { scale.animateTo(0.88f, spring(stiffness = Spring.StiffnessMedium)) }
                offsetY.animateTo(
                    targetValue = exitOffset(current!!.placement, bannerHeightPx + 48f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                current = null
                delay(80)
            }

            current = spec
            val startY = exitOffset(spec.placement, (bannerHeightPx + 60f).coerceAtLeast(120f))
            offsetY.snapTo(startY)
            alpha.snapTo(0f)
            scale.snapTo(0.88f)

            launch { alpha.animateTo(1f, tween(200)) }
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )

            dismissJob = scope.launch {
                delay(spec.durationMillis)
                launch { alpha.animateTo(0f, tween(180)) }
                launch { scale.animateTo(0.88f, spring(stiffness = Spring.StiffnessMedium)) }
                offsetY.animateTo(
                    targetValue = exitOffset(spec.placement, bannerHeightPx + 60f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                current = null
            }
        }
    }

    Box(modifier = modifier) {
        current?.let { spec ->
            val placementAlignment =
                if (spec.placement == NotificationPlacement.Top) contentAlignmentTop
                else contentAlignmentBottom

            Box(modifier = Modifier.align(placementAlignment)) {
                BannerLayout(
                    spec = spec,
                    offsetY = offsetY,
                    alpha = alpha,
                    scale = scale,
                    maxWidthFraction = maxWidthFraction,
                    onMeasured = { bannerHeightPx = it },
                    onDismiss = {
                        dismissJob?.cancel()
                        dismissJob = null
                        current = null
                    },
                    hostPaddingBottom = hostPaddingBottom
                )
            }
        }
    }
}

@Composable
private fun BannerLayout(
    spec: NotificationSpec,
    offsetY: Animatable<Float, AnimationVector1D>,
    alpha: Animatable<Float, AnimationVector1D>,
    scale: Animatable<Float, AnimationVector1D>,
    maxWidthFraction: Float,
    onMeasured: (heightPx: Int) -> Unit,
    onDismiss: () -> Unit,
    hostPaddingBottom: Dp = 0.dp,
) {
    val scope = rememberCoroutineScope()
    var measuredHeightPx by remember { mutableStateOf(0) }

    val timerProgress = remember { Animatable(1f) }
    LaunchedEffect(spec.id) {
        timerProgress.snapTo(1f)
        timerProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = spec.durationMillis.toInt(),
                easing = LinearEasing
            )
        )
    }

    val pointerModifier = Modifier.pointerInput(spec.id, spec.allowSwipeToDismiss) {
        if (!spec.allowSwipeToDismiss) return@pointerInput
        detectVerticalDragGestures(
            onVerticalDrag = { _, dragAmount ->
                scope.launch {
                    offsetY.snapTo(
                        (offsetY.value + dragAmount).coerceIn(
                            -1000f,
                            1000f
                        )
                    )
                }
            },
            onDragEnd = {
                scope.launch {
                    val threshold = measuredHeightPx * 0.35f
                    val placedBottom = spec.placement == NotificationPlacement.Bottom
                    val dismissed =
                        (placedBottom && offsetY.value > threshold) ||
                                (!placedBottom && offsetY.value < -threshold)

                    if (dismissed) {
                        val offScreen =
                            if (placedBottom) measuredHeightPx + 200f else -(measuredHeightPx + 200f)
                        offsetY.animateTo(
                            targetValue = offScreen,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        onDismiss()
                    } else {
                        offsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            },
            onDragCancel = {
                scope.launch {
                    offsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        )
    }

    val backgroundColor = spec.backgroundColor ?: MaterialTheme.colorScheme.inverseSurface
    val contentColor = spec.contentColor ?: MaterialTheme.colorScheme.inverseOnSurface
    val timerColor = MaterialTheme.colorScheme.inversePrimary

    Surface(
        modifier = Modifier
            .padding(bottom = hostPaddingBottom)
            .fillMaxWidth(maxWidthFraction)
            .graphicsLayer {
                translationY = offsetY.value
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            }
            .then(pointerModifier)
            .onSizeMeasured { _, h ->
                measuredHeightPx = h
                onMeasured(h)
            },
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                spec.icon?.let { icon ->
                    Box(contentAlignment = Alignment.Center) { icon() }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = spec.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                    )
                    spec.actions?.let { actions ->
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            content = actions
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(contentColor.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(timerProgress.value)
                        .fillMaxHeight()
                        .background(timerColor.copy(alpha = 0.55f))
                )
            }
        }
    }
}

@SuppressLint("SuspiciousModifierThen")
@Composable
private fun Modifier.onSizeMeasured(onSize: (Int, Int) -> Unit): Modifier = composed {
    this.then(Modifier.onSizeChanged { size -> onSize(size.width, size.height) })
}