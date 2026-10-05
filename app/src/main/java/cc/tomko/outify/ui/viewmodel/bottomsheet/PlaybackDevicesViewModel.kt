package cc.tomko.outify.ui.viewmodel.bottomsheet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.spirc.SpircWrapper
import cc.tomko.outify.core.model.Device
import cc.tomko.outify.core.model.DevicesResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class PlaybackDevicesViewModel @Inject constructor(
    private val spClient: SpClient,
    private val spircWrapper: SpircWrapper,
    private val json: Json,
) : ViewModel() {
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices

    fun selectDevice(device: Device) {
        val deviceId = device.id?.takeIf { it.isNotBlank() } ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val previousState = _devices.value

            _devices.update { list ->
                list.map {
                    if (it.id == deviceId) it.copy(isActive = true)
                    else it.copy(isActive = false)
                }
            }

            val success = runCatching { spClient.transferPlaybackDevice(deviceId) }.getOrDefault(false)

            if (!success) {
                _devices.value = previousState
            }
        }
    }

    suspend fun makeActive() = withContext(Dispatchers.IO) {
        runCatching { spircWrapper.activate() }
        runCatching { spircWrapper.transfer() }
    }

    suspend fun loadDevices() = withContext(Dispatchers.IO) {
        val raw = try {
            spClient.getDevices()
        } catch (e: Exception) {
            Log.w("PlaybackDevicesViewModel", "Device fetch failed", e)
            return@withContext
        } ?: return@withContext
        try {
            val parsed = json.decodeFromString<DevicesResponse>(raw)
            _devices.value = parsed.devices
        } catch (e: Exception) {
            Log.w("PlaybackDevicesViewModel", "Device parse failed", e)
        }
    }
}