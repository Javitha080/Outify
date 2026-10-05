package cc.tomko.outify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import cc.tomko.outify.data.database.LikedTrackWithTrack
import cc.tomko.outify.data.database.TrackWithArtists
import cc.tomko.outify.data.database.track.LikedTrackEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Dao
@Singleton
interface LikedDao {
    @Transaction
    @Query("SELECT * FROM liked_songs ORDER BY position ASC")
    fun observeLikedTracks(): Flow<List<LikedTrackWithTrack>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE trackId = :id)")
    suspend fun containsTrack(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE trackId = :id)")
    fun observeIsTrackLiked(id: String): Flow<Boolean>

    @Transaction
    @Query(
        """
        SELECT * FROM tracks
        WHERE id IN (:trackIds)
    """
    )
    suspend fun getTracksWithArtists(trackIds: List<String>): List<TrackWithArtists>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: LikedTrackEntity)

    @Query("DELETE FROM liked_songs WHERE trackId = :id")
    suspend fun delete(id: String)

    @Query("UPDATE liked_songs SET position = position + 1 WHERE position >= :fromPosition")
    suspend fun shiftPositions(fromPosition: Double)

    @Query("UPDATE liked_songs SET position = position - 1 WHERE position > :fromPosition")
    suspend fun shiftPositionsDown(fromPosition: Double)

    @Query("UPDATE liked_songs SET position = :newPosition WHERE trackId = :id")
    suspend fun updatePosition(id: String, newPosition: Double)

    /** Ordered URI snapshot — used for sync comparison */
    @Query("SELECT trackId FROM liked_songs ORDER BY position ASC")
    suspend fun getLikedIds(): List<String>

    /** Total count, even before metadata loads */
    @Query("SELECT COUNT(*) FROM liked_songs")
    fun observeCount(): Flow<Int>

    /** Total count (suspend version for sync progress) */
    @Query("SELECT COUNT(*) FROM liked_songs")
    suspend fun getCount(): Int

    /** Wipe all cached positions (used during full re-sync) */
    @Query("DELETE FROM liked_songs")
    suspend fun clearAll()

    /** Id window for triggering metadata fetch */
    @Query("SELECT trackId FROM liked_songs ORDER BY position ASC LIMIT :limit OFFSET :offset")
    suspend fun getIdsWindow(limit: Int, offset: Int): List<String>

    /**
     * Inner-joins liked_songs with tracks — only returns rows where metadata exists.
     */
    @Transaction
    @Query(
        """
        SELECT t.* FROM liked_songs ls
        INNER JOIN tracks t ON ls.trackId = t.id
        ORDER BY ls.position ASC
    """
    )
    fun observeLikedTracksWithDetails(): Flow<List<TrackWithArtists>>

    @Transaction
    @Query(
        """
        SELECT DISTINCT t.*
        FROM liked_songs ls
        INNER JOIN tracks t ON ls.trackId = t.id
        LEFT JOIN track_artists ta ON ta.trackId = t.id
        LEFT JOIN artists a ON a.artistId = ta.artistId
        WHERE
            t.name LIKE '%' || :query || '%'
            OR a.name LIKE '%' || :query || '%'
        ORDER BY ls.position ASC
    """
    )
    fun observeSearchLikedTracks(query: String): Flow<List<TrackWithArtists>>

    @Query("SELECT trackId FROM liked_songs")
    fun observeLikedIds(): Flow<List<String>>

    @Query(
        """
        SELECT t.id
        FROM liked_songs ls
        JOIN track_artists ta ON ta.trackId = ls.trackId
        JOIN tracks t ON t.id = ls.trackId
        WHERE ta.artistId = :artistId
        ORDER BY ls.position
    """
    )
    fun observeLikedIdsByArtist(artistId: String): Flow<List<String>>
}