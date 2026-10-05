package cc.tomko.outify.data.repository

import androidx.room.Transaction
import cc.tomko.outify.data.dao.TrackDao
import cc.tomko.outify.data.database.TrackEntity
import java.time.Clock
import javax.inject.Inject

class TrackRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val clock: Clock,
) {

    val CACHE_TTL_MS: Long = 5_000_000

    /**
     * Upserts the given TrackEntity
     */
    @Transaction
    suspend fun upsertTrack(
        track: TrackEntity,
        isLibrary: Boolean
    ) {
        val now = clock.millis()

        trackDao.insert(
            track.copy(
                isLibraryItem = isLibrary,
                lastUpdated = now,
                lastAccessed = now,
            )
        )
    }

    /**
     * Upserts multiple tracks
     */
    @Transaction
    suspend fun upsertTracks(
        tracks: List<TrackEntity>,
        isLibrary: Boolean
    ) {
        val now = clock.millis()

        trackDao.insertAll(
            tracks.map {
                it.copy(
                    isLibraryItem = isLibrary,
                    lastAccessed = now,
                    lastUpdated = now,
                )
            }
        )
    }

    /**
     * Updates the tracks lastAccessed timer
     */
    @Transaction
    suspend fun touchTrack(trackId: String) {
        trackDao.updateLastAccessed(trackId, clock.millis())
    }

    @Transaction
    suspend fun evictCache() {
        val cutoff = clock.millis() - CACHE_TTL_MS
        trackDao.evictOldCache(cutoff)
    }
}