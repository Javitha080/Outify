package cc.tomko.outify

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource

object MyIcons {
    val BrokenHeart: ImageVector
        @Composable
        get() = ImageVector.vectorResource(id = R.drawable.heart_broken)
}