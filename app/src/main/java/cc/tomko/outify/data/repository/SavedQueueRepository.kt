package cc.tomko.outify.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import cc.tomko.outify.data.queue.SavedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedQueueRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _queues = MutableStateFlow<List<SavedQueue>>(emptyList())
    val queues: StateFlow<List<SavedQueue>> = _queues.asStateFlow()

    private val _activeQueueId = MutableStateFlow<String?>(null)
    val activeQueueId: StateFlow<String?> = _activeQueueId.asStateFlow()

    init {
        scope.launch {
            val prefs = dataStore.data.first()
            _queues.value = decode(prefs[SettingsRepository.Keys.Queue.QUEUES])
            _activeQueueId.value = prefs[SettingsRepository.Keys.Queue.ACTIVE_ID]
        }
    }

    fun saveQueue(queue: SavedQueue) {
        _queues.update { list ->
            val i = list.indexOfFirst { it.id == queue.id }
            if (i >= 0) list.toMutableList().also { it[i] = queue }
            else list + queue
        }
        persist()
    }

    private var saveDebounceJob: kotlinx.coroutines.Job? = null

    fun debouncedSaveQueue(queue: SavedQueue, debounceMs: Long = 500L) {
        saveDebounceJob?.cancel()
        saveDebounceJob = scope.launch {
            delay(debounceMs)
            saveQueue(queue)
        }
    }

    fun deleteQueue(id: String) {
        _queues.update { it.filter { q -> q.id != id } }
        if (_activeQueueId.value == id) setActiveQueueId(null)
        persist()
    }

    fun renameQueue(id: String, newName: String) {
        _queues.update { list ->
            list.map { if (it.id == id) it.copy(name = newName) else it }
        }
        persist()
    }

    fun getQueue(id: String): SavedQueue? = _queues.value.find { it.id == id }

    fun playNext(queueId: String, trackUri: String) {
        _queues.update { list ->
            list.map { queue ->
                if (queue.id == queueId) {
                    val newTrackUris = listOf(trackUri) + queue.trackUris
                    queue.copy(trackUris = newTrackUris)
                } else {
                    queue
                }
            }
        }
        persist()
    }

    fun setActiveQueueId(id: String?) {
        _activeQueueId.value = id
        scope.launch {
            dataStore.edit { prefs ->
                if (id != null) prefs[SettingsRepository.Keys.Queue.ACTIVE_ID] = id
                else prefs.remove(SettingsRepository.Keys.Queue.ACTIVE_ID)
            }
        }
    }

    private fun persist() {
        val snapshot = _queues.value
        scope.launch {
            dataStore.edit { prefs ->
                prefs[SettingsRepository.Keys.Queue.QUEUES] = json.encodeToString(snapshot)
            }
        }
    }

    fun reload() {
        scope.launch {
            val prefs = dataStore.data.first()
            _queues.value = decode(prefs[SettingsRepository.Keys.Queue.QUEUES])
            _activeQueueId.value = prefs[SettingsRepository.Keys.Queue.ACTIVE_ID]
        }
    }

    private fun decode(raw: String?): List<SavedQueue> {
        raw ?: return emptyList()
        return try {
            json.decodeFromString<List<SavedQueue>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
