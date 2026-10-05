package cc.tomko.outify.data.setting

import androidx.compose.runtime.compositionLocalOf
import cc.tomko.outify.data.repository.InterfaceSettings

val LocalUiSettings = compositionLocalOf {
    InterfaceSettings()
}