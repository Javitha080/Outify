package cc.tomko.outify.data.metadata

import android.util.Log
import androidx.room.withTransaction
import cc.tomko.outify.core.model.Playlist
import cc.tomko.outify.core.model.PlaylistDiff
import cc.tomko.outify.core.model.PlaylistItem
import cc.tomko.outify.data.dao.PlaylistDao
import cc.tomko.outify.data.database.AppDatabase
import cc.tomko.outify.data.database.PlaylistEntity
import cc.tomko.outify.data.database.playlist.PlaylistItemEntity
import cc.tomko.outify.data.database.playlist.PlaylistWithItems
import cc.tomko.outify.data.database.playlist.toDomain
import cc.tomko.outify.data.database.playlist.toDomainOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistMetadataHelper @Inject constructor(
    private val db: AppDatabase,
    private val playlistDao: PlaylistDao,
    private val json: Json,
    private val nativeMetadata: NativeMetadata
) {
    /**
     * Retrieves a playlist by URI. If not present locally, fetches and persists.
     * Always tries to fetch remote to get latest revision and apply changes if available.
     */
    suspend fun getPlaylistMetadata(
        uri: String,
        allowCached: Boolean = true
    ): Playlist? = coroutineScope {
        if (uri.isBlank()) return@coroutineScope null

        val playlistId = uri.removePrefix("spotify:playlist:")
        val cached = if (allowCached) playlistDao.getPlaylistWithItems(playlistId) else null

        val remotePlaylist = runCatching {
            try {
                val raw = withContext(Dispatchers.IO) {
                    nativeMetadata.retryOnRateLimit {
                        nativeMetadata.fetchMetadata(uri)
                    }
                }

                withContext(Dispatchers.Default) {
                    json.decodeFromString<Playlist>(raw.toString())
                }
            } catch (e: RateLimitException) {
                Log.w("Metadata", "getPlaylistMetadata: rate-limited for $uri, giving up", e)
                null
            } catch (e: Exception) {
                Log.e("Metadata", "getPlaylistMetadata: failed for $uri", e)
                null
            }
        }.getOrNull()

        if (remotePlaylist == null && cached == null) return@coroutineScope null

        if (!allowCached) {
            remotePlaylist?.let {
                withContext(Dispatchers.IO) { persistPlaylist(it) }
            }
            return@coroutineScope remotePlaylist
        }

        if (remotePlaylist != null) {
            if (cached == null) {
                withContext(Dispatchers.IO) { persistPlaylist(remotePlaylist) }
                return@coroutineScope remotePlaylist
            }

            val storedRevision = cached.playlist.revision
            val remoteRevision = remotePlaylist.revision

            if (storedRevision == remoteRevision) {
                return@coroutineScope cached.toDomain()
            }

            val diff = remotePlaylist.diff
            if (diff != null) {
                withContext(Dispatchers.IO) { applyDiffAndPersist(cached, diff, remotePlaylist) }
            } else {
                withContext(Dispatchers.IO) { persistPlaylist(remotePlaylist) }
            }

            val updated =
                withContext(Dispatchers.IO) { playlistDao.getPlaylistWithItems(playlistId) }
            return@coroutineScope updated?.toDomain()
        } else {
            return@coroutineScope cached?.toDomain()
        }
    }

    /**
     * Observe playlist as Flow. Emits DB changes and triggers a background fetch to refresh state.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePlaylist(uri: String) = playlistDao
        .getPlaylistWithItemsFlow(uri.removePrefix("spotify:playlist:"))
        .mapLatest { it?.toDomain() }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePlaylists(uris: List<String>) = flow {
        if (uris.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val playlistIds = uris.map { it.removePrefix("spotify:playlist:") }

        val cached = withContext(Dispatchers.IO) {
            playlistDao.getPlaylistsWithItems(playlistIds)
        }.mapNotNull { it.toDomainOrNull() }

        Log.d("PlaylistMeta", "observePlaylists: cached.size=${cached.size} for ids=$playlistIds")
        emit(cached)

        // background refresh
        coroutineScope {
            uris.forEach { uri ->
                launch {
                    try {
                        getPlaylistMetadata(uri)
                    } catch (e: Exception) {
                        Log.w("PlaylistMeta", "Failed to refresh $uri", e)
                    }
                }
            }
        }

        emitAll(
            playlistDao.getPlaylistsWithItemsFlow(playlistIds)
                .mapLatest { list ->
                    Log.d("PlaylistMeta", "DB flow emitted ${list.size} playlists")
                    list.mapNotNull { it.toDomainOrNull() }
                }
        )
    }

    /**
     * Persist playlist entity & ordered items. Replaces existing items for playlist.
     */
    private suspend fun persistPlaylist(playlist: Playlist) {
        val playlistId = playlist.uri.substringAfterLast(":")
        val now = System.currentTimeMillis()

        val playlistEntity = PlaylistEntity(
            id = playlistId,
            uri = playlist.uri,
            ownerUsername = playlist.ownerUsername,
            revision = playlist.revision,
            name = playlist.attributes.name,
            description = playlist.attributes.description,
            pictureId = playlist.attributes.pictureId,
            isCollaborative = playlist.attributes.isCollaborative,
            isDeletedByOwner = playlist.attributes.isDeletedByOwner,
            timestamp = now,
        )

        val itemEntities = playlist.contents.mapIndexed { index, item ->
            PlaylistItemEntity(
                playlistId = playlistId,
                position = index,
                trackUri = item.uri,
                addedBy = item.attributes.addedBy,
                timestamp = item.attributes.timestamp,
                seenAt = item.attributes.seenAt,
                isPublic = item.attributes.isPublic
            )
        }

        db.withTransaction {
            playlistDao.upsertPlaylist(playlistEntity)
            playlistDao.deleteItems(playlistId)
            if (itemEntities.isNotEmpty()) playlistDao.insertItems(itemEntities)
        }
    }

    /**
     * Apply a PlaylistDiff (from remote) to the cached PlaylistWithItems and persist the new state.
     */
    private suspend fun applyDiffAndPersist(
        currentDb: PlaylistWithItems,
        diff: PlaylistDiff,
        remoteFull: Playlist
    ) {
        val playlistId = currentDb.playlist.id
        val now = System.currentTimeMillis()

        val currentItems =
            currentDb.items.sortedBy { it.position }.map { it.copy() }.toMutableList()

        fun remoteItemToEntity(remoteItem: PlaylistItem): PlaylistItemEntity {
            return PlaylistItemEntity(
                playlistId = playlistId,
                position = -1, // to be filled later by index
                trackUri = remoteItem.uri,
                addedBy = remoteItem.attributes.addedBy,
                timestamp = remoteItem.attributes.timestamp,
                seenAt = remoteItem.attributes.seenAt,
                isPublic = remoteItem.attributes.isPublic
            )
        }

        // Apply each operation sequentially
        diff.operations.forEach { op ->
            when (op.kind) {
                "add" -> {
                    val add = op.add ?: return@forEach
                    val insertPos = add.from_index.coerceIn(0, currentItems.size)
                    val toInsert = add.items.map { remoteItemToEntity(it) }
                    currentItems.addAll(insertPos, toInsert)
                }

                "rem" -> {
                    val rem = op.rem ?: return@forEach
                    val from = rem.from_index.coerceAtLeast(0)
                    val toIndexExclusive = (from + rem.length).coerceAtMost(currentItems.size)
                    if (from < toIndexExclusive) {
                        currentItems.subList(from, toIndexExclusive).clear()
                    }
                }

                "mov" -> {
                    val mov = op.mov ?: return@forEach
                    val from = mov.from_index.coerceAtLeast(0)
                    val length = mov.length.coerceAtLeast(0)
                    val to = mov.to_index.coerceAtLeast(0)

                    if (length == 0) return@forEach

                    // bounds
                    val end = (from + length).coerceAtMost(currentItems.size)
                    if (from >= end) return@forEach

                    val block = currentItems.subList(from, end).toList()
                    // remove block
                    currentItems.subList(from, end).clear()

                    // compute insertion index in the list *after removal*
                    val insertionIndex = when {
                        to <= from -> to.coerceIn(0, currentItems.size)
                        else -> (to - length).coerceIn(0, currentItems.size)
                    }

                    currentItems.addAll(insertionIndex, block)
                }

                "update_item_attributes" -> {
                    val u = op.updateItemAttributes ?: return@forEach

                    // if operation includes items, use them to update attributes for positions
                    if (u.items.isNotEmpty()) {
                        // It might provide a `position` or `position` optional. We treat `items` as replacements for a subrange.
                        val pos = u.position ?: 0
                        u.items.forEachIndexed { i, remoteItem ->
                            val targetIndex = pos + i
                            if (targetIndex in currentItems.indices) {
                                val existing = currentItems[targetIndex]
                                val updated = existing.copy(
                                    // keep trackUri (should be same), update attributes we know
                                    addedBy = remoteItem.attributes.addedBy,
                                    timestamp = remoteItem.attributes.timestamp,
                                    seenAt = remoteItem.attributes.seenAt,
                                    isPublic = remoteItem.attributes.isPublic,
                                )
                                currentItems[targetIndex] = updated
                            }
                        }
                    } else if (u.position != null && u.length != null) {
                        // no items list supplied — just clear attributes or keep as-is (skip)
                    }
                }

                "update_list_attributes" -> {
                    // Update playlist-level metadata later (we will persist remoteFull.playlist attributes)
                }

                else -> {
                    // unknown op -> ignore
                }
            }
        }

        // Re-assign positions
        val finalItems = currentItems.mapIndexed { idx, entity ->
            entity.copy(position = idx)
        }

        val updatedPlaylistEntity = PlaylistEntity(
            id = playlistId,
            uri = remoteFull.uri,
            ownerUsername = remoteFull.ownerUsername,
            revision = remoteFull.revision,
            name = remoteFull.attributes.name,
            description = remoteFull.attributes.description,
            pictureId = remoteFull.attributes.pictureId,
            isCollaborative = remoteFull.attributes.isCollaborative,
            isDeletedByOwner = remoteFull.attributes.isDeletedByOwner,
            timestamp = now
        )

        db.withTransaction {
            playlistDao.upsertPlaylist(updatedPlaylistEntity)
            playlistDao.deleteItems(playlistId)
            if (finalItems.isNotEmpty()) playlistDao.insertItems(finalItems)
        }
    }
}