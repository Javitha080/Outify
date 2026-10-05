package cc.tomko.outify.ui.viewmodel.settings

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.data.repository.InterfaceSettings
import cc.tomko.outify.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    val settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings: Flow<InterfaceSettings> =
        settingsRepository.interfaceSettings

    fun setMonochromeImages(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromeImages(enabled)
        }
    }

    fun setMonochromeAlbums(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromeAlbums(enabled)
        }
    }

    fun setMonochromeArtists(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromeArtists(enabled)
        }
    }

    fun setMonochromePlaylists(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromePlaylists(enabled)
        }
    }

    fun setMonochromeTracks(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromeTracks(enabled)
        }
    }

    fun setMonochromePlayer(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromePlayer(enabled)
        }
    }

    fun setMonochromeHeaders(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonochromeHeaders(enabled)
        }
    }

    fun setDynamicTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicTheme(enabled)
        }
    }

    fun setDynamicSystem(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicSystem(enabled)
        }
    }

    fun setAccentColor(color: Color) {
        viewModelScope.launch {
            settingsRepository.setAccentColor(color)
        }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPureBlack(enabled)
        }
    }

    fun setHighContrastCompat(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHighContrastCompat(enabled)
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            settingsRepository.setFontScale(scale)
        }
    }

    fun setExperimentalFloatingNav(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setExperimentalFloatingNav(enabled)
        }
    }

    fun setNavbarShowLabel(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNavbarShowLabel(enabled)
        }
    }
}
