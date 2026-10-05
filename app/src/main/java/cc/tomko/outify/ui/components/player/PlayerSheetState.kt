package cc.tomko.outify.ui.components.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

enum class PlayerSheetValue { Collapsed, Expanded }

@Stable
class PlayerSheetState(
    val draggableState: AnchoredDraggableState<PlayerSheetValue>
) {
    val currentValue get() = draggableState.currentValue
    val isExpanded get() = currentValue == PlayerSheetValue.Expanded

    /** 0f = fully collapsed, 1f = fully expanded */
    val progress: Float
        get() {
            val collapsed = draggableState.anchors.positionOf(PlayerSheetValue.Collapsed)
            val offset = draggableState.offset.takeIf { !it.isNaN() } ?: collapsed
            return if (collapsed == 0f) 0f else (1f - offset / collapsed).coerceIn(0f, 1f)
        }

    suspend fun expand() = draggableState.animateTo(PlayerSheetValue.Expanded)
    suspend fun collapse() = draggableState.animateTo(PlayerSheetValue.Collapsed)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberPlayerSheetState(
    initial: PlayerSheetValue = PlayerSheetValue.Collapsed
): PlayerSheetState {
    val density = LocalDensity.current

    val decaySpec = rememberSplineBasedDecay<Float>()

    val state = remember {
        AnchoredDraggableState(
            initialValue = initial,
            positionalThreshold = { distance: Float -> distance * 0.4f },
            velocityThreshold = { with(density) { 300.dp.toPx() } },
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            decayAnimationSpec = decaySpec
        )
    }

    return remember(state) { PlayerSheetState(state) }
}
