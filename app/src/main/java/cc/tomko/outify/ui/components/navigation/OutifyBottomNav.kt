package cc.tomko.outify.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavDestination(
    val id: String,
    val label: String,
    val route: Route,
    val icon: @Composable () -> Unit,
)

@Composable
fun OutifyBottomNav(
    items: List<NavDestination>,
    selectedId: String?,
    onItemSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 2.dp,
    contentPaddingHorizontal: Dp = 16.dp,
    contentPaddingVertical: Dp = 16.dp,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showLabels: Boolean = false,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        tonalElevation = tonalElevation,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = contentPaddingHorizontal, vertical = contentPaddingVertical)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.id == selectedId
                OutifyNavItem(
                    destination = item,
                    selected = isSelected,
                    onClick = { onItemSelected(item) },
                    selectedColor = selectedColor,
                    unselectedColor = unselectedColor,
                    showLabel = showLabels
                )
            }
        }
    }
}

@Composable
private fun OutifyNavItem(
    destination: NavDestination,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    unselectedColor: Color,
    showLabel: Boolean
) {
    val iconTint by animateColorAsState(if (selected) selectedColor else unselectedColor)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .widthIn(min = 56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .semantics { contentDescription = destination.label }
    ) {
        CompositionLocalProvider(LocalContentColor provides iconTint) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                destination.icon()
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = destination.label,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) selectedColor else unselectedColor
            )
        }
    }
}
