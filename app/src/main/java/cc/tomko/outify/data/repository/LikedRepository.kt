package cc.tomko.outify.data.repository

import android.util.Log
import androidx.room.withTransaction
import cc.tomko.outify.data.dao.AlbumDao
import cc.tomko.outify.data.dao.LikedDao
import cc.tomko.outify.data.database.AppDatabase
import cc.tomko.outify.data.database.TrackWithArtists
import cc.tomko.outify.data.database.album.AlbumWithArtists
import cc.tomko.outify.data.database.track.LikedTrackEntity
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.data.metadata.TrackMetadataHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class LikedRepository @Inject constructor(
    private val db: AppDatabase,
    private val likedDao: LikedDao,
    private val albumDao: AlbumDao,
    private val trackMetadataHelper: TrackMetadataHelper,
    private val metadata: Metadata,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "LikedRepository"
        private const val SUBSTRING_OFFSET = "spotify:track:".length
    }

    suspend fun syncLikedTracks(
        forceSync: Boolean = false,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): Boolean = withContext(Dispatchers.IO) {
        val pageSize = 20
        val perPageDelayMs = 100L
        val maxRetries = 3
        val initialBackoffMs = 500L

        try {
            try {
                if (!syncLikedUris() && !forceSync) return@withContext false
            } catch (t: Throwable) {
                Log.w(TAG, "syncLikedUris failed (continuing): ${t.message}", t)
            }

            yield()
            var offset = 0
            var anyFetched = false
            var batchNum = 0
            val total = likedDao.getCount()

            while (true) {
                val ids = likedDao.getIdsWindow(limit = pageSize, offset = offset)
                if (ids.isEmpty()) break

                batchNum++
                val uris = ids.map { "spotify:track:$it" }

                var attempt = 0
                var succeeded = false
                var backoff = initialBackoffMs

                while (attempt < maxRetries && !succeeded) {
                    try {
                        val fetched = trackMetadataHelper.getTrackMetadata(uris)

                        if (fetched.isNotEmpty()) {
                            anyFetched = true
                        }

                        succeeded = true
                    } catch (e: Exception) {
                        attempt++
                        val msg = e.message.orEmpty()
                        val isTransient = msg.contains("timeout", true) ||
                            msg.contains("unavailable", true) ||
                            msg.contains("rate", true) ||
                            msg.contains("network", true)
                        if (attempt >= maxRetries || !isTransient) {
                            Log.e(
                                TAG,
                                "Failed fetching metadata for liked tracks (offset=$offset).",
                                e
                            )
                            return@withContext false
                        } else {
                            Log.w(
                                TAG,
                                "Transient failure fetching metadata (offset=$offset), retrying in $backoff ms (attempt=$attempt).",
                                e
                            )
                            delay(backoff)
                            backoff = min(backoff * 2, 10_000L)
                        }
                    }
                }

                val processed = min(offset + pageSize, total)
                onProgress(processed, total)

                yield()

                // polite pause between pages
                delay(perPageDelayMs)
                offset += pageSize
            }

            Log.d(TAG, "syncLikedTracks finished; anyFetched=$anyFetched")
            return@withContext true
        } catch (t: Throwable) {
            Log.e("LikedRepository", "syncLikedTracks failed unexpectedly", t)
            return@withContext false
        }
    }

    /**
     * Pulls the URI list from the API and rebuilds liked_songs if it differs.
     * Returns true if anything changed.
     */
    suspend fun syncLikedUris(): Boolean {
        val remote = metadata.getLikedUris()
        val cached = likedDao.getLikedIds()

        if (remote.size == cached.size) {
            var allMatch = true
            for ((i, uri) in remote.withIndex()) {
                if (uri.length <= SUBSTRING_OFFSET || uri.substring(SUBSTRING_OFFSET) != cached[i]) {
                    allMatch = false
                    break
                }
            }
            if (allMatch) return false
        }

        Log.d(TAG, "Liked list changed (${cached.size} → ${remote.size}), resyncing")
        db.withTransaction {
            likedDao.clearAll()
            val now = System.currentTimeMillis()
            remote.forEachIndexed { i, uri ->
                if (uri.length <= SUBSTRING_OFFSET) return@forEachIndexed
                likedDao.insert(
                    LikedTrackEntity(
                        trackId = uri.substring(SUBSTRING_OFFSET),
                        position = i.toDouble(),
                        addedAt = now,
                    )
                )
            }
        }
        return true
    }

    /**
     * Emits TrackWithArtists rows for every liked song that has metadata cached.
     */
    fun observeLikedTracksWithDetails(): Flow<List<TrackWithArtists>> =
        likedDao.observeLikedTracksWithDetails()

    fun observeSearchLikedTracks(query: String): Flow<List<TrackWithArtists>> =
        likedDao.observeSearchLikedTracks(query)

    fun observeCount(): Flow<Int> = likedDao.observeCount()

    val likedCountState = likedDao.observeCount()
        .stateIn(scope, SharingStarted.Eagerly, 0)

    suspend fun isLiked(trackId: String): Boolean = likedDao.containsTrack(trackId)

    /**
     * Adds a track to the liked list (optimistic UI update)
     * Appends to the end of the list
     */
    suspend fun addLiked(trackId: String) {
        val currentCount = likedDao.getLikedIds().size
        likedDao.insert(
            LikedTrackEntity(
                trackId = trackId,
                position = currentCount.toDouble(),
                addedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Removes a track from the liked list (optimistic UI update)
     */
    suspend fun removeLiked(trackId: String) {
        likedDao.delete(trackId)
    }

    /**
     * Fetches (or confirms cached) metadata for a window of liked tracks.
     */
    suspend fun ensureWindowLoaded(offset: Int, size: Int) {
        val ids = likedDao.getIdsWindow(limit = size, offset = offset)
        if (ids.isNotEmpty()) {
            trackMetadataHelper.getTrackMetadata(ids.map { "spotify:track:$it" })
        }
    }

    suspend fun getAlbumsForTracks(tracks: List<TrackWithArtists>): Map<String, AlbumWithArtists?> {
        val albumIds = tracks.mapNotNull { it.track.albumId }.distinct()
        if (albumIds.isEmpty()) return emptyMap()
        return albumDao.getAlbumsWithArtists(albumIds).associateBy { it.album.albumId }
    }

    /**
     * Gets the index of a track by its URI in the liked tracks list.
     * Returns the 0-based index, or -1 if not found.
     */
    suspend fun getTrackIndex(trackUri: String): Int {
        val trackId = trackUri.substringAfterLast(":")
        val allIds = likedDao.getLikedIds()
        return allIds.indexOf(trackId)
    }
}