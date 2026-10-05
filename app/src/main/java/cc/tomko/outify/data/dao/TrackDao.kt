package cc.tomko.outify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import cc.tomko.outify.data.database.TrackEntity
import cc.tomko.outify.data.database.TrackFull
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Dao
@Singleton
interface TrackDao {
    /**
     * Inserts singular TrackEntity
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: TrackEntity)

    /**
     * Inserts multiple TrackEntities
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(track: List<TrackEntity>)

    @Query(
        """
        SELECT * FROM tracks
        WHERE trackUri IN (:uris)
    """
    )
    suspend fun getTracksByUris(uris: List<String>): List<TrackEntity>

    @Query(
        """
        SELECT albumId FROM tracks
        WHERE trackUri = :uri
    """
    )
    suspend fun getAlbumIdForTrack(uri: String): String?

    @Transaction
    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackFull(id: String): TrackFull?

    @Transaction
    @Query("SELECT * FROM tracks WHERE trackUri IN (:uris)")
    suspend fun getTracksFull(uris: List<String>): List<TrackFull>

    @Transaction
    @Query("SELECT * FROM tracks WHERE id = :id")
    fun observeTrackFull(id: String): Flow<TrackFull?>

    @Transaction
    @Query("SELECT * FROM tracks WHERE trackUri IN (:uris)")
    fun observeTracksFullFlow(uris: List<String>): Flow<List<TrackFull>>

    /**
     * Update the lastAccessed value of TrackEntity
     */
    @Query(
        """
        UPDATE tracks
        SET lastAccessed = :lastAccessed
        WHERE trackUri = :trackUri
    """
    )
    suspend fun updateLastAccessed(trackUri: String, lastAccessed: Long)

    /**
     * Updates the lastAccessed value of multiple TrackEntities
     */
    @Query(
        """
        UPDATE tracks
        SET lastAccessed = :lastAccessed
        WHERE trackUri IN (:trackUris)
    """
    )
    suspend fun updateLastAccessedBatch(trackUris: List<String>, lastAccessed: Long)

    /**
     * Evicts old cached entities
     */
    @Query(
        """
        DELETE FROM tracks
        WHERE isLibraryItem = 0
        AND lastAccessed < :cutoff
    """
    )
    suspend fun evictOldCache(cutoff: Long)

    @Query(
        """
        SELECT EXISTS(SELECT trackUri FROM tracks WHERE trackUri = :uri)
    """
    )
    suspend fun containsTrackByUri(uri: String): Boolean
}
