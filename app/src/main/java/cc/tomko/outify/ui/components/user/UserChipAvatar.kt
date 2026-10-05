package cc.tomko.outify.ui.components.user

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cc.tomko.outify.data.setting.LocalUiSettings
import cc.tomko.outify.ui.components.SmartImage

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserChipAvatar(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Circle.toShape())
    ) {
        Surface(
            tonalElevation = 12.dp
        ) {
            SmartImage(
                url = artworkUrl,
                modifier = Modifier
                    .fillMaxWidth(),
                monochrome = LocalUiSettings.current.monochromeArtists
            )
        }
    }
}