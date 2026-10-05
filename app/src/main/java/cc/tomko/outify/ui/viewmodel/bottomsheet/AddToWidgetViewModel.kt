package cc.tomko.outify.ui.viewmodel.bottomsheet

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.ViewModel
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.ui.widgets.PlaybackWidget
import cc.tomko.outify.widgetMediaPreference
import coil3.imageLoader
import coil3.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class WidgetData(
    val id: GlanceId,
    val uris: List<Track>
)

@HiltViewModel
class AddToWidgetViewModel @Inject constructor(
    private val metadata: Metadata,

    @ApplicationContext
    private val context: Context,
    private val json: Json,
) : ViewModel() {
    private val _widgetDataList = MutableStateFlow<List<WidgetData>>(emptyList())
    val widgetDataList: StateFlow<List<WidgetData>> = _widgetDataList.asStateFlow()

    suspend fun loadWidgets() {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(PlaybackWidget::class.java)
        val loadedData = ids.map { id ->
            val prefs = try {
                getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
            } catch (e: Exception) {
                null
            }

            val currentJson = prefs?.get(widgetMediaPreference(id)) ?: "[]"
            val uris = try {
                json.decodeFromString<List<String>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }

            val tracks = metadata.getTrackMetadata(uris)

            WidgetData(id, tracks)
        }

        _widgetDataList.value = loadedData
    }

    suspend fun addToWidget(uri: String, id: GlanceId) {
        withContext(Dispatchers.IO) {
            updateAppWidgetState(context, id) { prefs ->
                val currentJson = json.decodeFromString<List<String>>(
                    prefs[widgetMediaPreference(id)] ?: "[]"
                ).toMutableList()

                if (currentJson.contains(uri)) return@updateAppWidgetState

                currentJson.add(uri)
                prefs[widgetMediaPreference(id)] = json.encodeToString(currentJson)
            }
            PlaybackWidget().update(context, id)
        }
    }

    suspend fun removeFromWidget(uri: String, id: GlanceId) {
        withContext(Dispatchers.IO) {
            updateAppWidgetState(context, id) { prefs ->
                val currentJson = json.decodeFromString<List<String>>(
                    prefs[widgetMediaPreference(id)] ?: "[]"
                ).toMutableList()

                if (currentJson.remove(uri)) {
                    prefs[widgetMediaPreference(id)] = json.encodeToString(currentJson)
                }
            }
            PlaybackWidget().update(context, id)
        }
        loadWidgets()
    }

    /**
     * Saves the bitmap locally.
     */
    suspend fun saveBitmap(imageUrl: String?, uri: String) {
        if (imageUrl == null) return
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .diskCacheKey(uri)
            .build()

        context.imageLoader.execute(request)
    }
}